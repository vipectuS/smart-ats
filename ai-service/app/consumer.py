import asyncio
import json
import logging
from contextlib import suppress

from redis.asyncio import Redis
from redis.asyncio.client import PubSub
from pydantic import ValidationError

from app.config import Settings
from app.schemas.resume import ResumeParseMessage
from app.services.callbacks import BackendCallbackClient
from app.services.parser import BaseResumeParser, build_resume_parser


logger = logging.getLogger(__name__)


class ResumeQueueListener:
    def __init__(
        self,
        settings: Settings,
        parser: BaseResumeParser | None = None,
        callback_client: BackendCallbackClient | None = None,
        redis: Redis | None = None,
    ) -> None:
        self.settings = settings
        self.redis = redis
        self._task: asyncio.Task[None] | None = None
        self._running = False
        self._pubsub: PubSub | None = None
        self._subscribed = False
        self._owns_redis = redis is None
        self._parser = parser or build_resume_parser(settings)
        self._callback_client = callback_client or BackendCallbackClient(settings)

    async def start(self) -> None:
        if self._running:
            return

        if self.redis is None:
            self.redis = Redis.from_url(self.settings.redis_url, decode_responses=True)
        self._pubsub = self.redis.pubsub()
        self._running = True
        self._task = asyncio.create_task(self._consume_loop())
        logger.info(
            "Resume queue listener started with channel=%s and fallback queue=%s",
            self.settings.backend_queue_channel,
            self.settings.resume_queue_name,
        )

    async def stop(self) -> None:
        self._running = False
        if self._task:
            self._task.cancel()
            with suppress(asyncio.CancelledError):
                await self._task
        if self._pubsub:
            await self._pubsub.aclose()
        if self.redis:
            if self._owns_redis:
                await self.redis.aclose()
        await self._callback_client.close()
        logger.info("Resume queue listener stopped")

    async def _consume_loop(self) -> None:
        assert self.redis is not None

        while self._running:
            try:
                payload = await self._poll_payload()
                if payload is None:
                    continue

                await self._handle_payload(payload)
            except asyncio.CancelledError:
                raise
            except Exception:
                logger.exception("Unexpected error while polling Redis queue")
                await self._reset_pubsub()
                await asyncio.sleep(1)

    async def _poll_payload(self) -> str | None:
        pubsub_payload = await self._poll_pubsub_message()
        if pubsub_payload is not None:
            return pubsub_payload

        assert self.redis is not None
        item = await self.redis.brpop(self.settings.resume_queue_name, timeout=1)
        if item is None:
            return None

        _, payload = item
        return payload

    async def _poll_pubsub_message(self) -> str | None:
        assert self.redis is not None

        if self._pubsub is None:
            self._pubsub = self.redis.pubsub()

        if not self._subscribed:
            await self._pubsub.subscribe(self.settings.backend_queue_channel)
            self._subscribed = True
            logger.info("Subscribed to backend queue channel=%s", self.settings.backend_queue_channel)

        message = await self._pubsub.get_message(ignore_subscribe_messages=True, timeout=1.0)
        if message is None or message.get("type") != "message":
            return None

        payload = message.get("data")
        if isinstance(payload, bytes):
            return payload.decode()
        if isinstance(payload, str):
            return payload
        return json.dumps(payload)

    async def _reset_pubsub(self) -> None:
        if self._pubsub:
            await self._pubsub.aclose()
        if self.redis:
            self._pubsub = self.redis.pubsub()
        else:
            self._pubsub = None
        self._subscribed = False

    async def _handle_payload(self, payload: str) -> None:
        try:
            message = ResumeParseMessage.model_validate_json(payload)
        except ValidationError:
            logger.exception("Received invalid resume parsing task payload: %s", payload)
            return

        logger.info("Received resume parsing task for resumeId=%s", message.resume_id)

        try:
            profile = await self._parser.parse(message)
            await self._callback_client.submit_parsed_result(message.resume_id, profile)
            logger.info("Submitted parsed result callback for resumeId=%s", message.resume_id)
        except Exception as exc:
            logger.exception("Failed to parse or upload resumeId=%s", message.resume_id)
            try:
                await self._callback_client.report_failure(message.resume_id, str(exc))
                logger.info("Submitted parse failure callback for resumeId=%s", message.resume_id)
            except Exception:
                logger.exception("Failed to report parse failure for resumeId=%s", message.resume_id)
