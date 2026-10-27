"""MediConnect AI service: diabetes risk prediction and the MediBot assistant."""

from __future__ import annotations

import logging
from contextlib import asynccontextmanager
from typing import Literal

from fastapi import FastAPI
from pydantic import BaseModel, Field

from .assistant import MediBot, PolicyIndex
from .diabetes import MODEL_VERSION, DiabetesModel, Inputs

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")

state: dict = {}


@asynccontextmanager
async def lifespan(_: FastAPI):
    state["diabetes"] = DiabetesModel()
    state["bot"] = MediBot(PolicyIndex())
    yield
    state.clear()


app = FastAPI(title="MediConnect AI Service", version="1.0.0", lifespan=lifespan)


class DiabetesRequest(BaseModel):
    gender: Literal["Male", "Female", "Other"]
    age: int = Field(ge=1, le=110)
    hypertension: Literal[0, 1]
    heart_disease: Literal[0, 1]
    smoking_history: Literal["never", "former", "current", "not current", "ever", "No Info"]
    bmi: float = Field(ge=10, le=80)
    hba1c_level: float = Field(ge=3, le=15)
    blood_glucose_level: int = Field(ge=50, le=400)


class DiabetesResponse(BaseModel):
    probability: float
    model_probability: float
    risk_band: str
    model_version: str
    factors: list[str]
    recommendation: str


class ChatTurn(BaseModel):
    role: Literal["user", "assistant"]
    content: str


class ChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=1000)
    history: list[ChatTurn] = []


class Source(BaseModel):
    policy: str
    page: int
    snippet: str


class ChatResponse(BaseModel):
    reply: str
    sources: list[Source]
    suggested_specialization: str | None
    mode: str


@app.get("/health")
def health() -> dict:
    bot: MediBot = state["bot"]
    return {
        "status": "ok",
        "model_version": MODEL_VERSION,
        "policy_chunks": len(bot.index.chunks),
        "llm_enabled": bot.llm_enabled,
    }


@app.post("/predict/diabetes", response_model=DiabetesResponse)
def predict_diabetes(req: DiabetesRequest) -> DiabetesResponse:
    result = state["diabetes"].predict(Inputs(**req.model_dump()))
    return DiabetesResponse(
        probability=result.probability,
        model_probability=result.model_probability,
        risk_band=result.risk_band,
        model_version=MODEL_VERSION,
        factors=result.factors,
        recommendation=result.recommendation,
    )


@app.post("/assistant/chat", response_model=ChatResponse)
def chat(req: ChatRequest) -> ChatResponse:
    reply = state["bot"].chat(req.message.strip(), [t.model_dump() for t in req.history])
    return ChatResponse(
        reply=reply.reply,
        sources=[Source(**s) for s in reply.sources],
        suggested_specialization=reply.suggested_specialization,
        mode=reply.mode,
    )
