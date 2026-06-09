"""AI 语义提炼：口语文本 → 精炼备忘录正文。"""
import logging
from fastapi import APIRouter, Request, HTTPException
from pydantic import BaseModel, Field
from openai import OpenAI
from app.core.config import DEEPSEEK_API_KEY, DEEPSEEK_BASE_URL, DEEPSEEK_MODEL

logger = logging.getLogger(__name__)
router = APIRouter()

client = OpenAI(api_key=DEEPSEEK_API_KEY, base_url=DEEPSEEK_BASE_URL)

PROMPT = """你是智能备忘录助手。将用户输入的口语化文本提炼为精炼的备忘录正文。

规则：
- 去除口语语气词（提醒我、帮我记一下、那个、然后...）
- 保留时间信息（今天、明天、下午三点、周五等）
- 保留核心事件和对象
- 输出简洁，只保留必要信息
- 直接输出提炼后的文本，不要加任何前缀和解释

示例：
输入：提醒我下午五点进行mvp开发
输出：下午五点 mvp开发

输入：帮我记一下明天上午十点在三楼会议室开会讨论项目进度
输出：明天上午十点 三楼会议室 开会讨论项目进度

输入：记得后天要交房租
输出：后天 交房租"""


class RefineRequest(BaseModel):
    content: str = Field(min_length=1, max_length=10000)


class RefineResponse(BaseModel):
    refined: str


@router.post("/ai/refine")
async def refine(request: Request, body: RefineRequest):
    user_id = request.headers.get("X-User-Id")
    if not user_id:
        raise HTTPException(status_code=401, detail="未认证")

    logger.info("AI refine: userId=%s, len=%d", user_id, len(body.content))

    try:
        response = client.chat.completions.create(
            model=DEEPSEEK_MODEL,
            temperature=0.1,
            max_tokens=500,
            messages=[
                {"role": "system", "content": PROMPT},
                {"role": "user", "content": body.content},
            ],
        )

        refined = response.choices[0].message.content.strip()
        tokens = response.usage.total_tokens if response.usage else 0
        logger.info("AI refine done: refinedLen=%d, tokens=%d", len(refined), tokens)

        return {
            "code": "OK",
            "message": "success",
            "data": {"refined": refined, "tokens": tokens},
            "traceId": str(__import__("uuid").uuid4()),
        }

    except Exception as e:
        logger.exception("AI refine failed")
        raise HTTPException(status_code=500, detail=str(e))
