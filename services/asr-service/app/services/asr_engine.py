"""
ASR 转录引擎。

支持两种模式，通过 USE_MOCK 环境变量切换（默认使用真实引擎）。

真实引擎：阿里云 DashScope fun-asr
Mock 引擎：固定延迟 + 占位文本（调试用）
"""

import logging
import os
from app.core.config import DASHSCOPE_API_KEY, DASHSCOPE_BASE_URL
from app.services.file_service import get_presigned_url

logger = logging.getLogger(__name__)

USE_MOCK = os.getenv("ASR_ENGINE_MOCK", "false").lower() == "true"


async def transcribe(object_key: str) -> tuple[str, int]:
    """
    转录音频文件，返回 (转录文本, 音频时长秒数)。
    """
    if USE_MOCK:
        return await _mock_transcribe(object_key)
    return await _dashscope_transcribe(object_key)


# ==================== DashScope 真实引擎 ====================

async def _dashscope_transcribe(object_key: str) -> tuple[str, int]:
    """使用阿里云 DashScope fun-asr 进行真实转写。"""
    import dashscope
    from dashscope.audio.asr import Transcription
    from http import HTTPStatus

    dashscope.api_key = DASHSCOPE_API_KEY
    dashscope.base_http_api_url = DASHSCOPE_BASE_URL

    # 生成预签名 URL
    url = get_presigned_url(object_key, expiry_hours=24)
    logger.info("Submitting ASR task: object=%s, url=%s...", object_key, url[:80])

    # 提交任务
    task_response = Transcription.async_call(
        model='fun-asr',
        file_urls=[url],
        language_hints=['zh'],
    )

    logger.info("DashScope response: status=%s, output=%s, message=%s",
                task_response.status_code, task_response.output, task_response.message)

    if task_response.status_code != HTTPStatus.OK or task_response.output is None:
        raise RuntimeError(
            f"DashScope 提交失败: status={task_response.status_code}, "
            f"message={task_response.message}. "
            f"请确认: 1) DASHSCOPE_API_KEY 有效 2) 音频 URL 公网可访问"
        )

    task_id = task_response.output.task_id
    logger.info("ASR task submitted: taskId=%s, status=%s", task_id,
                task_response.output.task_status)

    # 等待完成
    result = Transcription.wait(task=task_id)

    if result.status_code != HTTPStatus.OK:
        raise RuntimeError(f"ASR transcription failed: {result.message}")

    # 提取文本
    text_parts = []
    duration = 0
    for r in result.output.results:
        if r.subtask_status == 'SUCCEEDED':
            transcript_url = r.transcription_url
            if transcript_url:
                text = await _fetch_transcript(transcript_url)
                text_parts.append(text)
            duration += r.duration or 0

    combined_text = "\n".join(text_parts) if text_parts else "(转写为空)"
    logger.info("ASR completed: taskId=%s, textLen=%d, duration=%ds",
                task_id, len(combined_text), duration)
    return combined_text, int(duration / 1000)  # 转为秒


async def _fetch_transcript(url: str) -> str:
    """下载转写结果 JSON，提取纯文本。"""
    import httpx
    async with httpx.AsyncClient(timeout=30) as client:
        resp = await client.get(url)
        resp.raise_for_status()
        data = resp.json()
        # DashScope 返回格式: {"transcripts": [{"text": "..."}, ...]}
        if isinstance(data, dict) and "transcripts" in data:
            return "".join(t.get("text", "") for t in data["transcripts"])
        return str(data)


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
