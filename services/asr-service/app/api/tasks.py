"""
ASR 任务 API。
"""
import asyncio
import logging
import uuid
from datetime import datetime, timezone

from fastapi import APIRouter, UploadFile, File, Form, Request, HTTPException
from app.core.database import get_pool
from app.services.file_service import upload_audio
from app.services.asr_engine import transcribe
from app.services.backfill import backfill_memo

logger = logging.getLogger(__name__)
router = APIRouter()


def _get_user_id(request: Request) -> str:
    """从 Gateway 注入的 header 中获取用户 ID。"""
    uid = request.headers.get("X-User-Id")
    if not uid:
        raise HTTPException(status_code=401, detail="未认证")
    return uid


def _trace_id() -> str:
    return str(uuid.uuid4())


@router.post("/asr/tasks")
async def create_task(
    request: Request,
    file: UploadFile = File(...),
    memo_id: str | None = Form(None),
):
    """上传音频文件并创建 ASR 任务。"""
    user_id = _get_user_id(request)
    audio_data = await file.read()

    # 上传到 MinIO
    object_key = upload_audio(user_id, file.filename or "recording.wav",
                               audio_data, file.content_type or "audio/wav")

    # 创建数据库记录
    task_id = uuid.uuid4()
    now = datetime.now(timezone.utc)
    pool = await get_pool()
    await pool.execute(
        """INSERT INTO asr_tasks (id, user_id, file_name, file_size, content_type,
           object_key, memo_id, status, created_at, updated_at)
           VALUES ($1,$2,$3,$4,$5,$6,$7,'pending',$8,$8)""",
        task_id, user_id, file.filename, len(audio_data),
        file.content_type, object_key, memo_id, now
    )

    # 启动后台异步处理
    asyncio.create_task(_process_task(task_id, user_id, object_key, memo_id))

    return {
        "code": "OK", "message": "success",
        "data": {
            "taskId": str(task_id),
            "status": "pending",
            "fileName": file.filename,
            "fileSize": len(audio_data),
            "memoId": memo_id,
            "createdAt": now.isoformat(),
        },
        "traceId": _trace_id(),
    }


@router.get("/asr/tasks/{task_id}")
async def get_task(task_id: str, request: Request):
    """查询 ASR 任务状态和结果。"""
    user_id = _get_user_id(request)
    pool = await get_pool()
    row = await pool.fetchrow(
        "SELECT * FROM asr_tasks WHERE id=$1 AND user_id=$2", uuid.UUID(task_id), user_id
    )
    if not row:
        raise HTTPException(status_code=404, detail="任务不存在")

    return {
        "code": "OK", "message": "success",
        "data": _row_to_dict(row),
        "traceId": _trace_id(),
    }


@router.get("/asr/tasks")
async def list_tasks(request: Request):
    """用户的任务列表。"""
    user_id = _get_user_id(request)
    pool = await get_pool()
    rows = await pool.fetch(
        "SELECT * FROM asr_tasks WHERE user_id=$1 ORDER BY created_at DESC LIMIT 50",
        user_id
    )
    return {
        "code": "OK", "message": "success",
        "data": {"items": [_row_to_dict(r) for r in rows]},
        "traceId": _trace_id(),
    }


async def _process_task(task_id, user_id, object_key, memo_id):
    """后台处理：转录 + 回填。"""
    pool = await get_pool()
    try:
        # status → processing
        await pool.execute(
            "UPDATE asr_tasks SET status='processing', updated_at=$2 WHERE id=$1",
            task_id, datetime.now(timezone.utc)
        )

        # 执行转录
        text, duration = await transcribe(object_key)

        # 回填到备忘录（如果指定了 memoId）
        if memo_id:
            await backfill_memo(memo_id, user_id, text)

        # status → completed
        now = datetime.now(timezone.utc)
        await pool.execute(
            """UPDATE asr_tasks SET status='completed', transcribed_text=$2,
               duration_seconds=$3, completed_at=$4, updated_at=$4 WHERE id=$1""",
            task_id, text, duration, now
        )
        logger.info("ASR task completed: id=%s", task_id)

    except Exception as e:
        logger.exception("ASR task failed: id=%s", task_id)
        await pool.execute(
            """UPDATE asr_tasks SET status='failed', error_message=$2,
               updated_at=$3 WHERE id=$1""",
            task_id, str(e), datetime.now(timezone.utc)
        )


def _row_to_dict(row) -> dict:
    return {
        "taskId": str(row["id"]),
        "status": row["status"],
        "fileName": row["file_name"],
        "fileSize": row["file_size"],
        "memoId": str(row["memo_id"]) if row["memo_id"] else None,
        "transcribedText": row["transcribed_text"],
        "durationSeconds": row["duration_seconds"],
        "errorMessage": row["error_message"],
        "createdAt": row["created_at"].isoformat() if row["created_at"] else None,
        "completedAt": row["completed_at"].isoformat() if row["completed_at"] else None,
    }
