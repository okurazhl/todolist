"""AI 自然语言解析：提取时间、事项、标题。"""
import json
import logging
from datetime import datetime, timezone, timedelta
from fastapi import APIRouter, Request, HTTPException
from pydantic import BaseModel, Field
from openai import OpenAI
from app.core.config import DEEPSEEK_API_KEY, DEEPSEEK_BASE_URL, DEEPSEEK_MODEL

logger = logging.getLogger(__name__)
router = APIRouter()

client = OpenAI(api_key=DEEPSEEK_API_KEY, base_url=DEEPSEEK_BASE_URL)

TZ = timezone(timedelta(hours=8))  # Asia/Shanghai

PROMPT = """你是智能备忘录助手。从用户的自然语言输入中提取结构化信息。

规则：
- 标题：提炼一个简短的备忘录标题（5-15字）
- 事项：核心事件描述，保留关键信息
- 时间：识别到的时间，转为 ISO 8601 格式（带 +08:00 时区）
- 如果用户没有指定日期，默认使用今天
- isPast：判断识别到的时间是否已经过去（以当前时间为准）
- 如果时间已过，suggestedTime 设为明天的同一时间（ISO 8601），suggestedLabel 设为可读的明天时间描述

输出严格 JSON 格式，不要 markdown 代码块，不要额外文字。
示例输入：今天晚上五点提醒我去吃饭
示例输出：{"title":"吃饭提醒","event":"去吃饭","datetime":"2026-06-08T17:00:00+08:00","isPast":false,"suggestedTime":null,"suggestedLabel":null}"""


class ParseRequest(BaseModel):
    content: str = Field(min_length=1, max_length=200)


@router.post("/ai/parse-nl")
async def parse_nl(request: Request, body: ParseRequest):
    user_id = request.headers.get("X-User-Id")
    if not user_id:
        raise HTTPException(status_code=401, detail="未认证")

    logger.info("AI parse-nl: userId=%s, text=%s", user_id, body.content)

    now = datetime.now(TZ)
    now_iso = now.strftime("%Y-%m-%dT%H:%M:%S+08:00")
    today_str = now.strftime("%Y年%m月%d日")

    prompt_with_ctx = PROMPT + f"\n\n当前时间：{now_iso}\n今天是：{today_str}"

    try:
        response = client.chat.completions.create(
            model=DEEPSEEK_MODEL,
            temperature=0.1,
            max_tokens=300,
            messages=[
                {"role": "system", "content": prompt_with_ctx},
                {"role": "user", "content": body.content},
            ],
            response_format={"type": "json_object"},
        )

        raw = response.choices[0].message.content.strip()
        logger.info("AI parse-nl raw: %s", raw)
        parsed = json.loads(raw)
        tokens = response.usage.total_tokens if response.usage else 0
        logger.info("AI parse-nl done: tokens=%d", tokens)

        return {
            "code": "OK",
            "message": "success",
            "data": {
                "title": parsed.get("title", ""),
                "event": parsed.get("event", ""),
                "datetime": parsed.get("datetime", ""),
                "isPast": bool(parsed.get("isPast", False)),
                "suggestedTime": parsed.get("suggestedTime"),
                "suggestedLabel": parsed.get("suggestedLabel"),
            },
            "traceId": str(__import__("uuid").uuid4()),
        }

    except json.JSONDecodeError as e:
        logger.exception("AI parse-nl JSON parse failed: %s", raw)
        raise HTTPException(status_code=500, detail=f"AI 返回格式错误: {e}")
    except Exception as e:
        logger.exception("AI parse-nl failed")
        raise HTTPException(status_code=500, detail=str(e))
