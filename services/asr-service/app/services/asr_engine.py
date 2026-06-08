"""
ASR 转录引擎。

默认使用 硅基流动 SiliconFlow SenseVoiceSmall（云端免费）。
可通过 ASR_ENGINE_MOCK=true 切换到 Mock 引擎。
"""

import logging
import os
import httpx
from app.core.config import SILICONFLOW_API_KEY as _SF_KEY
from app.services.file_service import get_presigned_url

logger = logging.getLogger(__name__)

USE_MOCK = os.getenv("ASR_ENGINE_MOCK", "false").lower() == "true"

# SiliconFlow 配置
SILICONFLOW_API_KEY = os.getenv("SILICONFLOW_API_KEY") or _SF_KEY
SILICONFLOW_ASR_URL = "https://api.siliconflow.cn/v1/audio/transcriptions"
SILICONFLOW_MODEL = "FunAudioLLM/SenseVoiceSmall"


async def transcribe(object_key: str) -> tuple[str, int]:
    """
    转录音频文件，返回 (转录文本, 音频时长秒数)。
    """
    if USE_MOCK:
        return await _mock_transcribe(object_key)
    return await _siliconflow_transcribe(object_key)


# ==================== SiliconFlow SenseVoice 引擎 ====================

async def _siliconflow_transcribe(object_key: str) -> tuple[str, int]:
    """使用硅基流动 SenseVoiceSmall 进行语音转写。"""
    from app.services.file_service import _get_client
    from app.core.config import MINIO_BUCKET

    # 从 MinIO 下载音频字节
    client = _get_client()
    response = client.get_object(MINIO_BUCKET, object_key)
    audio_bytes = response.read()
    response.close()
    response.release_conn()

    logger.info("Submitting to SiliconFlow: object=%s, size=%d, model=%s",
                object_key, len(audio_bytes), SILICONFLOW_MODEL)

    # 根据 object_key 推测文件扩展名
    ext = "webm" if object_key.endswith(".webm") else "wav"
    mime = "audio/webm" if ext == "webm" else "audio/wav"

    # 提交文件到 SiliconFlow
    async with httpx.AsyncClient(timeout=30) as http:
        resp = await http.post(
            SILICONFLOW_ASR_URL,
            headers={"Authorization": f"Bearer {SILICONFLOW_API_KEY}"},
            files={"file": (f"audio.{ext}", audio_bytes, mime)},
            data={"model": SILICONFLOW_MODEL},
        )

    if resp.status_code != 200:
        raise RuntimeError(
            f"SiliconFlow 转写失败: status={resp.status_code}, "
            f"body={resp.text[:200]}"
        )

    result = resp.json()
    text = result.get("text", "").strip()
    if not text:
        raise RuntimeError(f"SiliconFlow 返回空文本, raw={resp.text[:200]}")

    logger.info("SiliconFlow completed: textLen=%d, text=%s",
                len(text), text[:80])
    # duration 不精确，SenseVoice 不返回时长
    return text, 0


# ==================== Mock 引擎 ====================

async def _mock_transcribe(object_key: str) -> tuple[str, int]:
    import asyncio
    delay = 2.0
    logger.info("Mock transcribing: %s (delay=%ss)", object_key, delay)
    await asyncio.sleep(delay)
    text = (
        "【Mock 转录结果】\n"
        "这是模拟的语音转写文本。\n"
        f"音频文件: {object_key}\n"
        "设置 ASR_ENGINE_MOCK=false 使用真实引擎。"
    )
    return text, 20
