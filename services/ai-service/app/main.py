"""AI 语义服务入口。"""
import logging
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.api.refine import router as refine_router
from app.api.parse_nl import router as parse_nl_router

logging.basicConfig(level=logging.INFO, format='%(asctime)s %(levelname)s %(name)s %(message)s')
logger = logging.getLogger(__name__)

app = FastAPI(title="SmartMemo AI Service", version="0.1.0")

app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])
app.include_router(refine_router, prefix="/api/v1")
app.include_router(parse_nl_router, prefix="/api/v1")

@app.get("/actuator/health")
async def health():
    return {"status": "UP"}

logger.info("AI Service ready")
