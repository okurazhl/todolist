"""
智能备忘录 API 客户端
连接后端服务: Gateway(8080) → User/Memo/ASR/AI Services
"""
import json
import base64
import urllib.request
import urllib.error
from typing import Optional

BASE_URL = "http://localhost:8080/api/v1"

TOKEN_FILE = "token.json"


# ============= Token 管理 =============

def save_token(access: str, refresh: str):
    with open(TOKEN_FILE, "w") as f:
        json.dump({"access_token": access, "refresh_token": refresh}, f)


def load_token() -> Optional[str]:
    try:
        with open(TOKEN_FILE, "r") as f:
            return json.load(f).get("access_token")
    except Exception:
        return None


def decode_user_id(token: str) -> str:
    """从 JWT 中提取 userId (sub)"""
    try:
        payload = token.split(".")[1]
        payload += "=" * (4 - len(payload) % 4)
        data = json.loads(base64.urlsafe_b64decode(payload))
        return data.get("sub", "")
    except Exception:
        return ""


# ============= HTTP 请求 =============

def _req(method: str, path: str, data: dict = None, auth: bool = True) -> dict:
    url = f"{BASE_URL}{path}"
    body = json.dumps(data).encode() if data else None
    headers = {"Content-Type": "application/json"}
    if auth:
        token = load_token()
        if token:
            headers["Authorization"] = f"Bearer {token}"
    try:
        req = urllib.request.Request(url, data=body, headers=headers, method=method)
        with urllib.request.urlopen(req, timeout=15) as resp:
            return json.loads(resp.read())
    except urllib.error.HTTPError as e:
        err = json.loads(e.read())
        raise Exception(err.get("message", f"HTTP {e.code}"))


def _post_json(path: str, data: dict) -> dict:
    """无需认证的 POST"""
    return _req("POST", path, data, auth=False)


# ============= 认证 =============

def register(username: str, password: str) -> dict:
    return _post_json("/auth/register", {"username": username, "password": password})


def login(username: str, password: str) -> str:
    """登录成功返回 username"""
    resp = _post_json("/auth/login", {
        "username": username, "password": password,
        "deviceType": "desktop", "deviceName": "Todolist Desktop"
    })
    data = resp["data"]
    save_token(data["accessToken"], data["refreshToken"])
    return username


# ============= 备忘录 CRUD =============

def list_memos(status: str = "active") -> list:
    resp = _req("GET", f"/memos?status={status}&limit=50")
    return resp["data"]["items"]


def list_all_memos() -> list:
    """获取全部状态的备忘录"""
    resp = _req("GET", "/memos?limit=50")
    return resp["data"]["items"]


def create_memo(title: str, remind_at: str = None, content: str = None) -> dict:
    data = {"title": title}
    if content:
        data["content"] = content
    if remind_at:
        data["remindAt"] = remind_at
    resp = _req("POST", "/memos", data)
    return resp["data"]


def update_memo(memo_id: str, title: str = None, content: str = None,
                remind_at: str = None) -> dict:
    data = {}
    if title is not None:
        data["title"] = title
    if content is not None:
        data["content"] = content
    if remind_at is not None:
        data["remindAt"] = remind_at
    resp = _req("PATCH", f"/memos/{memo_id}", data)
    return resp["data"]


def complete_memo(memo_id: str) -> dict:
    resp = _req("POST", f"/memos/{memo_id}/complete")
    return resp["data"]


def uncomplete_memo(memo_id: str) -> dict:
    resp = _req("DELETE", f"/memos/{memo_id}/complete")
    return resp["data"]


def delete_memo(memo_id: str) -> None:
    _req("DELETE", f"/memos/{memo_id}")


def get_reminder_count() -> int:
    resp = _req("GET", "/memos/reminder-count")
    return resp["data"]["count"]


# ============= ASR 语音转写 =============

def upload_audio(filepath: str) -> str:
    """上传音频文件，返回 taskId"""
    import os
    boundary = "----TodolistBoundary"
    fname = os.path.basename(filepath)
    with open(filepath, "rb") as f:
        audio = f.read()

    body = (f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; "
            f"filename=\"{fname}\"\r\nContent-Type: audio/wav\r\n\r\n").encode()
    body += audio
    body += f"\r\n--{boundary}--\r\n".encode()

    url = f"{BASE_URL}/asr/tasks"
    token = load_token()
    headers = {"Authorization": f"Bearer {token}",
               "Content-Type": f"multipart/form-data; boundary={boundary}"}
    req = urllib.request.Request(url, data=body, headers=headers, method="POST")
    resp = json.loads(urllib.request.urlopen(req, timeout=30).read())
    return resp["data"]["taskId"]


def poll_asr(task_id: str, timeout: int = 30) -> str:
    """轮询 ASR 任务，返回转写文本"""
    import time
    token = load_token()
    deadline = time.time() + timeout
    while time.time() < deadline:
        time.sleep(2)
        resp = _req("GET", f"/asr/tasks/{task_id}")
        status = resp.get("status") or resp.get("data", {}).get("status", "")
        if status == "completed":
            text = resp.get("transcribedText") or resp.get("data", {}).get("transcribedText", "")
            return text
        if status == "failed":
            raise Exception(resp.get("errorMessage") or resp.get("data", {}).get("errorMessage", "转写失败"))
    raise Exception("转写超时")


# ============= AI 自然语言解析 =============

def parse_natural_language(text: str) -> dict:
    resp = _req("POST", "/ai/parse-nl", {"content": text})
    return resp["data"]


# ============= 推送 =============

def get_unread_push_count() -> int:
    resp = _req("GET", "/push/unread-count")
    return resp["data"]["count"]


def list_push_messages() -> list:
    resp = _req("GET", "/push/messages")
    return resp["data"]["items"]
