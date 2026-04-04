package com.smartats.backend.queue

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.stereotype.Component

@Component
class ResumeQueueProducer(
    private val resumeParseRedisTemplate: RedisTemplate<String, ResumeParseMessage>,
    private val resumeParseTopic: ChannelTopic,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun publish(message: ResumeParseMessage) {
        logger.info("Publishing resume parse message for resumeId={} to channel={}", message.resumeId, resumeParseTopic.topic)
        resumeParseRedisTemplate.convertAndSend(resumeParseTopic.topic, message)
    }
}