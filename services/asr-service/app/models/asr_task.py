from dataclasses import dataclass
from datetime import datetime
from uuid import UUID


@dataclass
class AsrTask:
    id: UUID
    user_id: UUID
    file_name: str
    file_size: int
    content_type: str | None
    object_key: str
    memo_id: UUID | None
    status: str  # pending | processing | completed | failed
    transcribed_text: str | None
    duration_seconds: int | None
    error_message: str | None
    created_at: datetime
    updated_at: datetime
    completed_at: datetime | None
