"""
MinIO 文件服务。
上传录音文件到 MinIO，复用 smartmemo-attachments bucket。
"""

import io
import uuid
import logging
from minio import Minio
from app.core.config import MINIO_ENDPOINT, MINIO_ACCESS_KEY, MINIO_SECRET_KEY, MINIO_BUCKET

logger = logging.getLogger(__name__)

_client: Minio | None = None


def _get_client() -> Minio:
    global _client
    if _client is None:
        _client = Minio(MINIO_ENDPOINT, access_key=MINIO_ACCESS_KEY,
                        secret_key=MINIO_SECRET_KEY, secure=False)
        _ensure_bucket(_client)
    return _client


def _ensure_bucket(client: Minio):
    import minio.error
    try:
        if not client.bucket_exists(MINIO_BUCKET):
            client.make_bucket(MINIO_BUCKET)
            logger.info("Bucket created: %s", MINIO_BUCKET)
    except minio.error.S3Error as e:
        if e.code != "BucketAlreadyOwnedByYou":
            raise


def upload_audio(user_id: str, file_name: str, data: bytes, content_type: str) -> str:
    """上传音频到 MinIO，返回 object_key。"""
    object_key = f"asr/{user_id}/{uuid.uuid4()}-{file_name}"
    client = _get_client()
    data_stream = io.BytesIO(data)
    client.put_object(
        MINIO_BUCKET, object_key,
        data=data_stream, length=len(data),
        content_type=content_type or "application/octet-stream"
    )
    logger.info("Audio uploaded: object=%s, size=%d", object_key, len(data))
    return object_key
