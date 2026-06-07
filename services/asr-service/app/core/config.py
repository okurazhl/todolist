import os

DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "postgresql://smartmemo:smartmemo_dev@localhost:5432/smartmemo"
)

MINIO_ENDPOINT = os.getenv("MINIO_ENDPOINT", "localhost:9000")
MINIO_ACCESS_KEY = os.getenv("MINIO_ACCESS_KEY", "minioadmin")
MINIO_SECRET_KEY = os.getenv("MINIO_SECRET_KEY", "minioadmin_dev")
MINIO_BUCKET = os.getenv("MINIO_BUCKET", "smartmemo-attachments")

MEMO_SERVICE_URL = os.getenv("MEMO_SERVICE_URL", "http://localhost:8082")

ASR_MOCK_DELAY = float(os.getenv("ASR_MOCK_DELAY", "2.0"))
