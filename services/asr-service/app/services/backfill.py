"""
回填服务：将转写文本写入关联的备忘录。
"""

import logging
import httpx
from app.core.config import MEMO_SERVICE_URL

logger = logging.getLogger(__name__)


async def backfill_memo(memo_id: str, user_id: str, text: str):
    """调用 memo-service API 将转写文本追加到备忘录末尾。"""
    url = f"{MEMO_SERVICE_URL}/api/v1/memos/{memo_id}"
    headers = {
        "Content-Type": "application/json",
        "X-User-Id": user_id,
    }
    # 获取现有备忘录内容
    async with httpx.AsyncClient(timeout=10) as client:
        resp = await client.get(url, headers=headers)
        if resp.status_code != 200:
            logger.warning("Failed to get memo %s: %s", memo_id, resp.status_code)
            return

        memo = resp.json()
        existing = memo.get("data", {}).get("content") or ""
        new_content = existing + ("\n\n" if existing else "") + f"【语音转写】\n{text}"

        patch_resp = await client.patch(url, headers=headers, json={"content": new_content})
        if patch_resp.status_code == 200:
            logger.info("Backfill success: memoId=%s, textLen=%d", memo_id, len(text))
        else:
            logger.warning("Backfill failed: memoId=%s, status=%s", memo_id, patch_resp.status_code)
