"""
ASR 语音转写服务入口。
"""

import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from app.core.database import init_db, close_db
from app.api.tasks import router as tasks_router

logging.basicConfig(level=logging.INFO, format='%(asctime)s %(levelname)s %(name)s %(message)s')
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting ASR service...")
    await init_db()
    yield
    await close_db()
    logger.info("ASR service stopped.")


app = FastAPI(title="SmartMemo ASR Service", version="0.1.0", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(tasks_router, prefix="/api/v1")


@app.get("/actuator/health")
async def health():
    return {"status": "UP"}
