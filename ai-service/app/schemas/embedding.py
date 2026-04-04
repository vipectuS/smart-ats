from pydantic import BaseModel, ConfigDict, Field


class EmbeddingRequest(BaseModel):
    model_config = ConfigDict(extra="ignore")

    text: str = Field(min_length=1)


class EmbeddingResponse(BaseModel):
    embedding: list[float]
    dimensions: int
