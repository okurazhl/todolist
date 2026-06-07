"""
ASR 转录引擎。

MVP 阶段使用 Mock 引擎模拟转写行为，
后续替换为 FunASR 只需修改此文件，对外接口不变。
"""

import asyncio
import logging
from app.core.config import ASR_MOCK_DELAY

logger = logging.getLogger(__name__)


async def transcribe(object_key: str) -> tuple[str, int]:
    """
    转录音频文件，返回 (转录文本, 音频时长秒数)。

    当前为 Mock 实现：
    - 模拟 1-3 秒处理延迟
    - 返回占位文本（后续接入 FunASR）
    """
    logger.info("Mock transcribing: %s (delay=%ss)", object_key, ASR_MOCK_DELAY)
    await asyncio.sleep(ASR_MOCK_DELAY)

    # Mock 转写结果
    text = (
        "【Mock 转录结果】\n"
        "这是模拟的语音转写文本。MVP 阶段占位。\n"
        f"音频文件: {object_key}\n"
        "正式版本将接入 FunASR 引擎进行真实转写。"
    )
    duration = int(ASR_MOCK_DELAY * 10)  # 模拟音频时长
    return text, duration
