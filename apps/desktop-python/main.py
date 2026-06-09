import json
import os
import sys
from dataclasses import asdict, dataclass, field
from datetime import datetime, timedelta
import tkinter as tk
from tkinter import Canvas, StringVar, Toplevel, messagebox, ttk, simpledialog

try:
    import api_client as api
    HAS_BACKEND = True
except ImportError:
    HAS_BACKEND = False


APP_TITLE = "TodoList"
BASE_DIR = os.path.dirname(sys.executable) if getattr(sys, "frozen", False) else os.path.dirname(os.path.abspath(__file__))
DATA_FILE = os.path.join(BASE_DIR, "tasks.json")
SETTINGS_FILE = os.path.join(BASE_DIR, "settings.json")
TIME_FORMAT = "%Y-%m-%d %H:%M"

MIN_WIDTH = 430
MIN_HEIGHT = 330
NORMAL_SIZE = (600, 900)
EXPANDED_SIZE = (950, 760)
EXPANDED_MIN_SIZE = (900, 620)
TEXT_SCALING = 1.35

BG = "#182334"
TOP = "#132131"
PANEL = "#223445"
PANEL_2 = "#2B4153"
PANEL_3 = "#344B5E"
LINE = "#4D6477"
LINE_SOFT = "#3B5063"
TEXT = "#EEF7FF"
MUTED = "#B8C7D6"
DISABLED = "#8593A2"
ACCENT = "#45D4C4"
ACCENT_DARK = "#1D6F72"
RED = "#FF6F6B"
YELLOW = "#FFD35C"
GREEN = "#62D58C"

PRIORITIES = {
    "high": {"level": 3, "color": RED},
    "medium": {"level": 2, "color": YELLOW},
    "low": {"level": 1, "color": GREEN},
}
PRIORITY_ORDER = ("high", "medium", "low")


def enable_high_dpi():
    if sys.platform != "win32":
        return
    try:
        import ctypes

        ctypes.windll.shcore.SetProcessDpiAwareness(2)
    except Exception:
        try:
            ctypes.windll.user32.SetProcessDPIAware()
        except Exception:
            pass


def rounded_rect(canvas, x1, y1, x2, y2, radius, **kwargs):
    points = [
        x1 + radius, y1,
        x2 - radius, y1,
        x2, y1,
        x2, y1 + radius,
        x2, y2 - radius,
        x2, y2,
        x2 - radius, y2,
        x1 + radius, y2,
        x1, y2,
        x1, y2 - radius,
        x1, y1 + radius,
        x1, y1,
    ]
    return canvas.create_polygon(points, smooth=True, **kwargs)


@dataclass
class Task:
    id: str
    content: str
    created_at: str
    due_at: str
    priority: str = "medium"
    completed: bool = False
    completed_at: str = ""
    reminded: bool = False
    reminder: bool = True
    cleared: bool = False
    backend_id: str = ""  # 后端 memo ID 映射


# ===== Task ↔ Memo 格式转换 =====

def due_to_iso(due_str: str) -> str:
    """'2026-06-01 09:24' → '2026-06-01T09:24:00+08:00'"""
    if not due_str:
        return None
    try:
        dt = datetime.strptime(due_str, TIME_FORMAT)
        return dt.strftime("%Y-%m-%dT%H:%M:%S+08:00")
    except Exception:
        return None


def iso_to_due(iso_str: str) -> str:
    """'2026-06-01T09:24:00+08:00' → '2026-06-01 09:24'"""
    if not iso_str:
        return ""
    try:
        dt = datetime.fromisoformat(iso_str.replace("Z", "+00:00"))
        return dt.strftime(TIME_FORMAT)
    except Exception:
        return ""


PRIORITY_TAG_MAP = {"high": "高优先级", "medium": "中优先级", "low": "低优先级"}


def memo_to_task(memo: dict) -> Task:
    """后端 Memo → 本地 Task"""
    due = iso_to_due(memo.get("remindAt", ""))
    priority = "medium"
    for p, tag in PRIORITY_TAG_MAP.items():
        if tag in (memo.get("title", "") + (memo.get("content") or "")):
            priority = p
            break
    return Task(
        id=f"api-{memo['id'][:12]}",
        content=memo.get("title", ""),
        created_at=iso_to_due(memo.get("createdAt", "")),
        due_at=due,
        priority=priority,
        completed=memo.get("status") == "completed",
        completed_at=iso_to_due(memo.get("updatedAt", "")) if memo.get("status") == "completed" else "",
        backend_id=memo["id"],
    )


class TodoApp:
    def __init__(self, root):
        self.root = root
        self.settings = self.load_settings()
        self.expanded = self.settings.get("mode", "normal") == "expanded"
        self.alpha = float(self.settings.get("alpha", 0.97))
        self.text_scaling = float(self.settings.get("text_scaling", TEXT_SCALING))
        self.root.tk.call("tk", "scaling", self.text_scaling)
        self.logged_in = HAS_BACKEND and self.backend_login()
        self.login_skipped = False
        self.voice_recording = False
        self.login_error = ""
        self.login_user = StringVar(value="")
        self.login_pass = StringVar(value="")
        size = self.current_mode_size()

        self.root.title(APP_TITLE)
        self.root.overrideredirect(True)
        self.root.attributes("-topmost", True)
        self.root.attributes("-alpha", self.alpha)
        self.root.configure(bg=BG)
        self.root.geometry(f"{size[0]}x{size[1]}+80+120")
        self.root.minsize(MIN_WIDTH, MIN_HEIGHT)

        self.canvas = Canvas(self.root, bg=BG, highlightthickness=0, bd=0)
        self.canvas.pack(fill="both", expand=True)

        self.tasks: list[Task] = []
        self.hitboxes = []
        self.pointer_mode = None
        self.drag_start = (0, 0)
        self.window_start = (0, 0)
        self.size_start = size
        self.topmost = True
        self.settings_panel = None

        self.priority_var = StringVar(value="medium")
        due_default = datetime.now() + timedelta(hours=1)
        self.due_date_var = StringVar(value=due_default.strftime("%Y-%m-%d"))
        self.due_time_var = StringVar(value=due_default.strftime("%H:%M"))
        self.reminder_var = StringVar(value="待提醒")
        self.task_placeholder = "添加任务"
        self.task_has_placeholder = False
        self.expanded_task_has_placeholder = False

        self.setup_styles()
        self.create_embedded_widgets()
        self.load_tasks()

        self.canvas.bind("<Configure>", lambda _event: self.render())
        self.canvas.bind("<ButtonPress-1>", self.on_press)
        self.canvas.bind("<B1-Motion>", self.on_motion)
        self.canvas.bind("<ButtonRelease-1>", self.on_release)
        self.root.bind("<Double-Button-1>", self.on_double_click)

        self.render()
        self.schedule_reminder_check()

    def setup_styles(self):
        style = ttk.Style(self.root)
        style.theme_use("clam")
        style.configure(
            "Todo.TCombobox",
            fieldbackground=PANEL_2,
            background=PANEL_2,
            foreground=TEXT,
            arrowcolor=MUTED,
            bordercolor=LINE,
            lightcolor=LINE,
            darkcolor=LINE,
            padding=4,
        )
        style.map("Todo.TCombobox", fieldbackground=[("readonly", PANEL_2)], foreground=[("readonly", TEXT)])

    def create_embedded_widgets(self):
        # 登录输入框
        self.login_user_entry = tk.Entry(
            self.canvas, bg="#EFF4F8", fg="#1F2A36", insertbackground="#1F2A36",
            relief="flat", bd=0, font=("Microsoft YaHei UI", 12), width=16,
        )
        self.login_pass_entry = tk.Entry(
            self.canvas, bg="#EFF4F8", fg="#1F2A36", insertbackground="#1F2A36",
            relief="flat", bd=0, font=("Microsoft YaHei UI", 12), width=16, show="*",
        )
        self.login_btn = tk.Button(
            self.canvas, text="登录", command=self._do_login, bg=ACCENT, fg="#182334",
            font=("Microsoft YaHei UI", 12, "bold"), relief="flat", cursor="hand2",
            padx=16, pady=4, bd=0,
        )
        self.offline_btn = tk.Button(
            self.canvas, text="离线使用", command=self._skip_login, bg=PANEL_3, fg=TEXT,
            font=("Microsoft YaHei UI", 10), relief="flat", cursor="hand2",
            padx=10, pady=4, bd=0,
        )
        self.task_entry = tk.Entry(
            self.canvas,
            bg="#EFF4F8",
            fg="#1F2A36",
            insertbackground="#1F2A36",
            relief="flat",
            bd=0,
            font=("Microsoft YaHei UI", 16),
        )
        self.task_entry.bind("<Return>", lambda _event: self.add_task())
        self.task_entry.bind("<FocusIn>", lambda _event: self.clear_task_placeholder())
        self.task_entry.bind("<FocusOut>", lambda _event: self.show_task_placeholder())
        self.show_task_placeholder()

        self.expanded_task_entry = tk.Entry(
            self.canvas,
            bg=PANEL_2,
            fg=TEXT,
            insertbackground=TEXT,
            relief="flat",
            bd=0,
            font=("Microsoft YaHei UI", 14),
        )
        self.expanded_task_entry.bind("<Return>", lambda _event: self.add_task(expanded=True))
        self.expanded_task_entry.bind("<FocusIn>", lambda _event: self.clear_expanded_task_placeholder())
        self.expanded_task_entry.bind("<FocusOut>", lambda _event: self.show_expanded_task_placeholder())
        self.show_expanded_task_placeholder()

        self.date_entry = tk.Entry(
            self.canvas,
            textvariable=self.due_date_var,
            bg=PANEL_2,
            fg=TEXT,
            insertbackground=TEXT,
            relief="flat",
            bd=0,
            font=("Consolas", 13),
        )
        self.time_entry = tk.Entry(
            self.canvas,
            textvariable=self.due_time_var,
            bg=PANEL_2,
            fg=TEXT,
            insertbackground=TEXT,
            relief="flat",
            bd=0,
            font=("Consolas", 13),
        )
        self.reminder_combo = ttk.Combobox(
            self.canvas,
            textvariable=self.reminder_var,
            values=("待提醒", "不提醒"),
            state="readonly",
            style="Todo.TCombobox",
            font=("Microsoft YaHei UI", 13),
        )

    def render(self):
        self.canvas.delete("all")
        self.hitboxes.clear()
        width = max(self.root.winfo_width(), MIN_WIDTH)
        height = max(self.root.winfo_height(), MIN_HEIGHT)

        rounded_rect(self.canvas, 0, 0, width, height, 18, fill=BG, outline="#9AB0C4", width=1)
        self.draw_top_bar(width)

        if self.expanded:
            self.draw_expanded(width, height)
        else:
            self.draw_compact(width, height)

        self.draw_resize_grip(width, height)
        self.apply_window_shape()

    def draw_top_bar(self, width):
        rounded_rect(self.canvas, 0, 0, width, 58, 18, fill=TOP, outline="")
        self.canvas.create_rectangle(0, 40, width, 58, fill=TOP, outline="")

        for col_x in (18, 25):
            for y in (18, 25, 32, 39):
                self.canvas.create_oval(col_x - 1.5, y - 1.5, col_x + 1.5, y + 1.5, fill=MUTED, outline="")

        self.canvas.create_oval(58, 18, 84, 44, fill=ACCENT, outline="")
        self.canvas.create_line(65, 30, 71, 36, 79, 25, fill=BG, width=3, capstyle="round", joinstyle="round")
        self.canvas.create_text(96, 31, text=APP_TITLE, anchor="w", fill=ACCENT, font=("Microsoft YaHei UI", 20, "bold"))

        self.add_hitbox(width - 142, 14, width - 112, 44, self.toggle_topmost)
        self.draw_pin(width - 127, 29, fill=MUTED if self.topmost else DISABLED)
        self.add_hitbox(width - 94, 14, width - 64, 44, self.open_settings)
        self.draw_menu(width - 79, 29)
        self.add_hitbox(width - 48, 14, width - 18, 44, self.close)
        self.draw_close(width - 33, 29)

    def draw_compact(self, width, height):
        margin = 16
        input_y = 74
        add_w = 74
        gap = 16
        input_x = 24
        input_w = max(width - input_x - add_w - gap - 24, 220)

        list_y = 136

        # 未登录时显示登录表单
        show_login = HAS_BACKEND and not self.logged_in and not self.login_skipped
        if show_login:
            self.canvas.create_text(24, input_y, text="登录云端同步", anchor="w",
                                    fill=TEXT, font=("Microsoft YaHei UI", 13, "bold"))

            # 用户名行
            uy = input_y + 30
            self.canvas.create_text(24, uy + 16, text="用户", anchor="w",
                                    fill=MUTED, font=("Microsoft YaHei UI", 11))
            bx = 70
            rounded_rect(self.canvas, bx, uy + 2, bx + 150, uy + 32, 6,
                         fill="#F2F6FA", outline="#DDE7F0", width=1)
            self.canvas.create_window(bx + 8, uy + 17, anchor="w",
                                      window=self.login_user_entry, width=134, height=24)

            # 密码行
            py = uy + 38
            self.canvas.create_text(24, py + 16, text="密码", anchor="w",
                                    fill=MUTED, font=("Microsoft YaHei UI", 11))
            rounded_rect(self.canvas, bx, py + 2, bx + 150, py + 32, 6,
                         fill="#F2F6FA", outline="#DDE7F0", width=1)
            self.canvas.create_window(bx + 8, py + 17, anchor="w",
                                      window=self.login_pass_entry, width=134, height=24)

            # 按钮行
            by = py + 38
            self.canvas.create_window(24, by + 16, anchor="w",
                                      window=self.login_btn, width=80, height=34)
            self.canvas.create_window(112, by + 16, anchor="w",
                                      window=self.offline_btn, width=90, height=34)

            # 错误
            if self.login_error:
                self.canvas.create_text(24, by + 50, text=self.login_error, anchor="w",
                                        fill=RED, font=("Microsoft YaHei UI", 9))
                list_y = by + 80
            else:
                list_y = by + 55

            self.login_pass_entry.bind("<Return>", lambda _e: self._do_login())
        else:
            # 已登录/离线模式：正常输入框 + 语音按钮
            voice_w = 42
            input_w2 = max(width - input_x - add_w - voice_w - gap * 2 - 24, 180)
            rounded_rect(self.canvas, input_x, input_y, input_x + input_w2, input_y + 48, 8,
                         fill="#F2F6FA", outline="#DDE7F0", width=1)
            self.canvas.create_window(input_x + 18, input_y + 24, anchor="w",
                                      window=self.task_entry, width=input_w2 - 28, height=30)

            # 语音按钮 (Canvas 手绘)
            vx = input_x + input_w2 + 8
            self.draw_mic_button(vx, input_y, voice_w, 48)

            add_x = vx + voice_w + 8
            self.draw_add_button(add_x, input_y, add_w, 48, lambda: self.add_task())
            list_y = 136
        footer_h = 46
        footer_y = height - footer_h - 16
        list_h = max(150, footer_y - list_y - 12)
        rounded_rect(self.canvas, margin, list_y, width - margin, list_y + list_h, 12, fill=PANEL, outline=LINE_SOFT, width=1)

        rows = self.visible_tasks()
        row_h = 45
        max_rows = max(int((list_h - 14) // row_h), 1)
        for index, task in enumerate(rows[:max_rows]):
            y = list_y + 7 + index * row_h
            self.draw_compact_row(task, margin + 12, y, width - margin - 12, row_h)
        if len(rows) > max_rows:
            self.canvas.create_text(width - 30, list_y + list_h - 12, text=f"+{len(rows) - max_rows}", fill=MUTED, font=("Microsoft YaHei UI", 12))

        self.draw_clear_button(margin, footer_y, width - margin, footer_y + footer_h)

    def draw_compact_row(self, task, x1, y, x2, row_h):
        y_mid = y + row_h / 2
        self.canvas.create_line(x1, y + row_h, x2, y + row_h, fill=LINE_SOFT)
        self.draw_checkbox(x1 + 4, y_mid - 11, task.completed, lambda task_id=task.id: self.toggle_complete(task_id))

        content_fill = DISABLED if task.completed else TEXT
        self.canvas.create_text(x1 + 42, y_mid, text=task.content, anchor="w", fill=content_fill, font=("Microsoft YaHei UI", 15, "bold"))
        if task.completed:
            self.canvas.create_line(x1 + 42, y_mid + 1, min(x1 + 210, x2 - 210), y_mid + 1, fill=DISABLED)

        chip_w = 138
        chip_x = max(x2 - 286, x1 + 214)
        rounded_rect(self.canvas, chip_x, y_mid - 18, chip_x + chip_w, y_mid + 18, 8, fill=PANEL_2, outline=LINE_SOFT, width=1)
        self.draw_calendar(chip_x + 18, y_mid)
        self.canvas.create_text(chip_x + 36, y_mid, text=self.short_due(task.due_at), anchor="w", fill=MUTED, font=("Consolas", 12))

        self.draw_priority_dot(x2 - 68, y_mid, task.priority)
        self.draw_trash(x2 - 24, y_mid, lambda task_id=task.id: self.delete_task(task_id))

    def draw_expanded(self, width, height):
        margin = 16
        form_y = 78
        self.draw_expanded_form(width, form_y)

        table_y = 164
        history_h = min(190, max(140, int(height * 0.25)))
        table_h = max(275, height - table_y - history_h - 32)
        history_y = table_y + table_h + 14
        self.draw_task_table(margin, table_y, width - margin, table_y + table_h)
        self.draw_history_panel(margin, history_y, width - margin, height - 16)

    def draw_expanded_form(self, width, y):
        x = 24
        input_w = max(220, min(260, int(width * 0.25)))
        self.canvas.create_text(x, y + 7, text="任务内容", anchor="w", fill=TEXT, font=("Microsoft YaHei UI", 13, "bold"))
        rounded_rect(self.canvas, x, y + 28, x + input_w, y + 74, 8, fill=PANEL_2, outline=LINE, width=1)
        self.canvas.create_window(x + 14, y + 51, anchor="w", window=self.expanded_task_entry, width=input_w - 24, height=28)

        due_x = x + input_w + 24
        self.canvas.create_text(due_x, y + 7, text="提醒时间", anchor="w", fill=TEXT, font=("Microsoft YaHei UI", 13, "bold"))
        rounded_rect(self.canvas, due_x, y + 28, due_x + 144, y + 74, 8, fill=PANEL_2, outline=LINE, width=1)
        self.canvas.create_window(due_x + 12, y + 51, anchor="w", window=self.date_entry, width=102, height=28)
        self.draw_calendar(due_x + 124, y + 51)
        time_x = due_x + 146
        rounded_rect(self.canvas, time_x, y + 28, time_x + 98, y + 74, 8, fill=PANEL_2, outline=LINE, width=1)
        self.canvas.create_window(time_x + 10, y + 51, anchor="w", window=self.time_entry, width=54, height=28)
        self.draw_clock(time_x + 76, y + 51)

        reminder_x = time_x + 120
        reminder_w = 162
        self.canvas.create_text(reminder_x, y + 7, text="提醒", anchor="w", fill=TEXT, font=("Microsoft YaHei UI", 13, "bold"))
        rounded_rect(self.canvas, reminder_x, y + 28, reminder_x + reminder_w, y + 74, 8, fill=PANEL_2, outline=LINE, width=1)
        self.draw_bell(reminder_x + 22, y + 51)
        self.canvas.create_window(reminder_x + 42, y + 51, anchor="w", window=self.reminder_combo, width=110, height=30)

        priority_x = reminder_x + reminder_w + 24
        self.canvas.create_text(priority_x, y + 7, text="优先级", anchor="w", fill=TEXT, font=("Microsoft YaHei UI", 13, "bold"))
        self.draw_priority_group(priority_x + 48, y + 51, self.priority_var.get(), interactive=True)

        add_x = width - 74
        self.draw_add_button(add_x, y + 28, 52, 46, lambda: self.add_task(expanded=True))

    def draw_task_table(self, x1, y1, x2, y2):
        rounded_rect(self.canvas, x1, y1, x2, y2, 14, fill=PANEL, outline=LINE_SOFT, width=1)
        header_h = 52
        self.canvas.create_rectangle(x1 + 12, y1 + 12, x2 - 12, y1 + header_h, fill=PANEL_2, outline="")

        col_content = x1 + 48
        col_due = x1 + int((x2 - x1) * 0.42)
        col_reminder = x1 + int((x2 - x1) * 0.61)
        col_priority = x1 + int((x2 - x1) * 0.78)
        col_action = x2 - 58

        for text, col in (("任务内容", col_content), ("提醒时间", col_due), ("提醒", col_reminder), ("优先级", col_priority), ("操作", col_action)):
            self.canvas.create_text(col, y1 + 31, text=text, anchor="w" if text != "操作" else "center", fill=TEXT, font=("Microsoft YaHei UI", 13, "bold"))

        rows = self.visible_tasks()
        row_h = 46
        rows_y = y1 + header_h
        footer_h = 48
        max_rows = max(int((y2 - rows_y - footer_h - 8) // row_h), 1)
        for index, task in enumerate(rows[:max_rows]):
            y = rows_y + index * row_h
            self.draw_table_row(task, x1 + 12, y, x2 - 12, row_h, col_content, col_due, col_reminder, col_priority, col_action)

        self.draw_clear_button(x1 + 14, y2 - footer_h - 8, x2 - 14, y2 - 8)

    def draw_table_row(self, task, x1, y, x2, row_h, col_content, col_due, col_reminder, col_priority, col_action):
        y_mid = y + row_h / 2
        self.canvas.create_line(x1, y + row_h, x2, y + row_h, fill=LINE_SOFT)
        self.draw_checkbox(x1 + 10, y_mid - 11, task.completed, lambda task_id=task.id: self.toggle_complete(task_id))

        fill = DISABLED if task.completed else TEXT
        self.canvas.create_text(col_content + 28, y_mid, text=task.content, anchor="w", fill=fill, font=("Microsoft YaHei UI", 13, "bold"))
        self.canvas.create_text(col_due, y_mid, text=self.short_due(task.due_at), anchor="w", fill=fill, font=("Consolas", 13))
        if task.reminder and not task.completed:
            self.draw_bell(col_reminder + 4, y_mid)
            reminder_text = "待提醒"
            reminder_fill = TEXT
        else:
            reminder_text = "--"
            reminder_fill = MUTED
        self.canvas.create_text(col_reminder + 24, y_mid, text=reminder_text, anchor="w", fill=reminder_fill, font=("Microsoft YaHei UI", 13))
        self.draw_priority_dot(col_priority + 62, y_mid, task.priority)
        self.draw_trash(col_action, y_mid, lambda task_id=task.id: self.delete_task(task_id))

    def draw_history_panel(self, x1, y1, x2, y2):
        rounded_rect(self.canvas, x1, y1, x2, y2, 14, fill=PANEL, outline=LINE_SOFT, width=1)
        self.draw_clock(x1 + 22, y1 + 18, color=MUTED)
        self.canvas.create_text(x1 + 40, y1 + 18, text="历史任务", anchor="w", fill=TEXT, font=("Microsoft YaHei UI", 13, "bold"))

        header_y = y1 + 34
        rounded_rect(self.canvas, x1 + 12, header_y, x2 - 12, header_y + 39, 7, fill=PANEL_2, outline="", width=0)
        col_content = x1 + 66
        col_created = x1 + int((x2 - x1) * 0.40)
        col_done = x1 + int((x2 - x1) * 0.67)
        col_priority = x2 - 118
        for text, col in (("任务内容", col_content), ("设定时间", col_created), ("提醒时间", col_done), ("优先级", col_priority)):
            self.canvas.create_text(col, header_y + 19, text=text, anchor="w", fill=TEXT, font=("Microsoft YaHei UI", 12, "bold"))

        rows = [task for task in self.tasks if task.completed]
        row_h = 34
        max_rows = max(int((y2 - header_y - 48) // row_h), 1)
        for index, task in enumerate(rows[:max_rows]):
            y = header_y + 39 + index * row_h
            y_mid = y + row_h / 2
            self.canvas.create_line(x1 + 12, y + row_h, x2 - 12, y + row_h, fill=LINE_SOFT)
            self.draw_clock(x1 + 26, y_mid, color=MUTED)
            self.canvas.create_text(col_content, y_mid, text=task.content, anchor="w", fill=TEXT, font=("Microsoft YaHei UI", 12))
            self.canvas.create_text(col_created, y_mid, text=self.short_created(task.created_at), anchor="w", fill=TEXT, font=("Consolas", 12))
            self.canvas.create_text(col_done, y_mid, text=self.short_created(task.completed_at), anchor="w", fill=TEXT, font=("Consolas", 12))
            self.draw_priority_dot(col_priority + 54, y_mid, task.priority, radius=5)

    def draw_mic_button(self, x, y, width, height):
        """Canvas 手绘麦克风按钮"""
        fill = RED if self.voice_recording else ACCENT
        outline = "#C0392B" if self.voice_recording else ACCENT_DARK
        text = "🔴" if self.voice_recording else "🎙️"
        rounded_rect(self.canvas, x, y, x + width, y + height, 8,
                     fill=fill, outline=outline, width=1)
        self.canvas.create_text(x + width // 2, y + height // 2,
                                text=text, fill="#182334",
                                font=("Microsoft YaHei UI", 16))
        if not self.voice_recording:
            self.add_hitbox(x, y, x + width, y + height, self._voice_input)

    def draw_add_button(self, x, y, width, height, command):
        rounded_rect(self.canvas, x, y, x + width, y + height, 10, fill=ACCENT, outline="#6BE8DC", width=1)
        cx = x + width / 2
        cy = y + height / 2
        self.canvas.create_line(cx - 10, cy, cx + 10, cy, fill=TEXT, width=3, capstyle="round")
        self.canvas.create_line(cx, cy - 10, cx, cy + 10, fill=TEXT, width=3, capstyle="round")
        self.add_hitbox(x, y, x + width, y + height, command)

    def draw_clear_button(self, x1, y1, x2, y2):
        rounded_rect(self.canvas, x1, y1, x2, y2, 9, fill=PANEL_2, outline=LINE_SOFT, width=1)
        cx = (x1 + x2) / 2 - 42
        cy = (y1 + y2) / 2
        self.draw_trash(cx, cy, None, color=ACCENT)
        self.canvas.create_text(cx + 22, cy, text="清除已完成", anchor="w", fill=ACCENT, font=("Microsoft YaHei UI", 14, "bold"))
        self.add_hitbox(x1, y1, x2, y2, self.clear_completed)

    def draw_checkbox(self, x, y, checked, command):
        if checked:
            rounded_rect(self.canvas, x, y, x + 22, y + 22, 3, fill=ACCENT, outline="#7EF3E7", width=1)
            self.canvas.create_line(x + 5, y + 11, x + 10, y + 16, x + 17, y + 6, fill=TEXT, width=2.2, capstyle="round", joinstyle="round")
        else:
            rounded_rect(self.canvas, x, y, x + 22, y + 22, 3, fill=PANEL, outline="#DCEAFF", width=2)
        self.add_hitbox(x - 4, y - 4, x + 26, y + 26, command)

    def draw_priority_group(self, x, y, active, interactive=False, small=False):
        radius = 5 if small else 7
        spacing = 20 if small else 28
        dot_defs = (("high", RED), ("medium", YELLOW), ("low", GREEN))
        start_x = x - spacing
        for index, (priority, color) in enumerate(dot_defs):
            cx = start_x + index * spacing
            if priority == active:
                self.canvas.create_oval(cx - radius - 3, y - radius - 3, cx + radius + 3, y + radius + 3, outline="#9EE6E0", width=1)
            self.canvas.create_oval(cx - radius, y - radius, cx + radius, y + radius, fill=color, outline="")
            if interactive:
                self.add_hitbox(cx - 12, y - 12, cx + 12, y + 12, lambda p=priority: self.set_priority(p))

    def draw_priority_dot(self, x, y, priority, radius=7):
        color = PRIORITIES.get(priority, PRIORITIES["medium"])["color"]
        self.canvas.create_oval(x - radius, y - radius, x + radius, y + radius, fill=color, outline="")

    def draw_trash(self, x, y, command, color=TEXT):
        self.canvas.create_line(x - 7, y - 8, x + 7, y - 8, fill=color, width=2)
        self.canvas.create_line(x - 3, y - 11, x + 3, y - 11, fill=color, width=2)
        self.canvas.create_rectangle(x - 6, y - 6, x + 6, y + 10, outline=color, width=2)
        self.canvas.create_line(x - 2, y - 3, x - 2, y + 7, fill=color, width=1)
        self.canvas.create_line(x + 2, y - 3, x + 2, y + 7, fill=color, width=1)
        if command:
            self.add_hitbox(x - 14, y - 16, x + 14, y + 16, command)

    def draw_calendar(self, x, y, color=MUTED):
        self.canvas.create_rectangle(x - 7, y - 8, x + 7, y + 8, outline=color, width=1.6)
        self.canvas.create_line(x - 7, y - 3, x + 7, y - 3, fill=color, width=1.6)
        self.canvas.create_line(x - 3, y - 11, x - 3, y - 5, fill=color, width=1.6)
        self.canvas.create_line(x + 3, y - 11, x + 3, y - 5, fill=color, width=1.6)

    def draw_clock(self, x, y, color=MUTED):
        self.canvas.create_oval(x - 8, y - 8, x + 8, y + 8, outline=color, width=1.6)
        self.canvas.create_line(x, y, x, y - 5, fill=color, width=1.6)
        self.canvas.create_line(x, y, x + 5, y + 3, fill=color, width=1.6)

    def draw_bell(self, x, y):
        self.canvas.create_arc(x - 8, y - 9, x + 8, y + 9, start=20, extent=140, outline=TEXT, width=1.6, style="arc")
        self.canvas.create_line(x - 7, y + 3, x + 7, y + 3, fill=TEXT, width=1.6)
        self.canvas.create_line(x - 5, y + 3, x - 3, y - 5, fill=TEXT, width=1.6)
        self.canvas.create_line(x + 5, y + 3, x + 3, y - 5, fill=TEXT, width=1.6)
        self.canvas.create_oval(x - 2, y + 5, x + 2, y + 9, fill=TEXT, outline="")

    def draw_pin(self, x, y, fill=MUTED):
        self.canvas.create_line(x - 6, y - 8, x + 6, y + 4, fill=fill, width=1.8)
        self.canvas.create_line(x - 7, y + 7, x + 2, y - 2, fill=fill, width=1.8)
        self.canvas.create_polygon(x - 4, y - 8, x + 8, y - 4, x + 3, y + 1, x - 8, y - 3, outline=fill, fill="", width=1.4)

    def draw_menu(self, x, y):
        for offset in (-6, 0, 6):
            self.canvas.create_line(x - 8, y + offset, x + 8, y + offset, fill=MUTED, width=1.8, capstyle="round")

    def draw_close(self, x, y):
        self.canvas.create_line(x - 7, y - 7, x + 7, y + 7, fill=TEXT, width=1.8, capstyle="round")
        self.canvas.create_line(x + 7, y - 7, x - 7, y + 7, fill=TEXT, width=1.8, capstyle="round")

    def draw_resize_grip(self, width, height):
        for offset in (7, 13, 19):
            self.canvas.create_line(width - offset, height - 3, width - 3, height - offset, fill="#6A7D8F", width=1)

    def add_hitbox(self, x1, y1, x2, y2, command):
        self.hitboxes.append((x1, y1, x2, y2, command))

    def on_press(self, event):
        for x1, y1, x2, y2, command in reversed(self.hitboxes):
            if x1 <= event.x <= x2 and y1 <= event.y <= y2:
                command()
                return
        width = self.root.winfo_width()
        height = self.root.winfo_height()
        self.drag_start = (event.x_root, event.y_root)
        self.window_start = (self.root.winfo_x(), self.root.winfo_y())
        self.size_start = (width, height)
        if event.x >= width - 24 and event.y >= height - 24:
            self.pointer_mode = "resize"
        elif event.y <= 58:
            self.pointer_mode = "drag"
        else:
            self.pointer_mode = None

    def on_motion(self, event):
        if self.pointer_mode == "drag":
            dx = event.x_root - self.drag_start[0]
            dy = event.y_root - self.drag_start[1]
            self.root.geometry(f"+{self.window_start[0] + dx}+{self.window_start[1] + dy}")
        elif self.pointer_mode == "resize":
            dx = event.x_root - self.drag_start[0]
            dy = event.y_root - self.drag_start[1]
            width = max(MIN_WIDTH, self.size_start[0] + dx)
            height = max(MIN_HEIGHT, self.size_start[1] + dy)
            self.root.geometry(f"{width}x{height}")

    def on_release(self, _event):
        if self.pointer_mode == "resize":
            self.save_current_size()
        self.pointer_mode = None

    def on_double_click(self, event):
        if event.y <= 58:
            self.toggle_mode()

    def visible_tasks(self):
        return [task for task in self.tasks if not task.cleared]

    def set_priority(self, priority):
        self.priority_var.set(priority)
        self.render()

    def add_task(self, expanded=False):
        if expanded:
            content = self.get_expanded_task_content()
        else:
            content = self.get_task_content()
        due_at = self.get_due_string()

        if not content:
            messagebox.showinfo(APP_TITLE, "请输入任务内容。")
            return
        if not self.parse_time(due_at):
            messagebox.showinfo(APP_TITLE, "提醒时间格式应为 YYYY-MM-DD HH:MM。")
            return

        # 后端模式：通过 API 创建
        if HAS_BACKEND and self.logged_in:
            try:
                memo = api.create_memo(
                    title=content,
                    remind_at=due_to_iso(due_at),
                )
                task = memo_to_task(memo)
                task.priority = self.priority_var.get()
                self.tasks.append(task)
            except Exception as e:
                messagebox.showwarning(APP_TITLE, f"同步失败: {e}")
                # 本地回退
                self.tasks.append(Task(
                    id=datetime.now().strftime("%Y%m%d%H%M%S%f"),
                    content=content,
                    created_at=datetime.now().strftime(TIME_FORMAT),
                    due_at=due_at,
                    priority=self.priority_var.get(),
                    reminder=self.reminder_var.get() == "待提醒",
                ))
        else:
            self.tasks.append(Task(
                id=datetime.now().strftime("%Y%m%d%H%M%S%f"),
                content=content,
                created_at=datetime.now().strftime(TIME_FORMAT),
                due_at=due_at,
                priority=self.priority_var.get(),
                reminder=self.reminder_var.get() == "待提醒",
            ))
        self.task_entry.delete(0, "end")
        self.expanded_task_entry.delete(0, "end")
        self.show_task_placeholder()
        self.show_expanded_task_placeholder()
        due_default = datetime.now() + timedelta(hours=1)
        self.due_date_var.set(due_default.strftime("%Y-%m-%d"))
        self.due_time_var.set(due_default.strftime("%H:%M"))
        self.save_tasks()
        self.render()

    def get_task_content(self):
        if self.task_has_placeholder:
            return ""
        return self.task_entry.get().strip()

    def show_task_placeholder(self):
        if self.task_entry.get().strip():
            return
        self.task_has_placeholder = True
        self.task_entry.configure(fg="#6D7986")
        self.task_entry.delete(0, "end")
        self.task_entry.insert(0, self.task_placeholder)

    def clear_task_placeholder(self):
        if not self.task_has_placeholder:
            return
        self.task_has_placeholder = False
        self.task_entry.configure(fg="#1F2A36")
        self.task_entry.delete(0, "end")

    def get_expanded_task_content(self):
        if self.expanded_task_has_placeholder:
            return ""
        return self.expanded_task_entry.get().strip()

    def show_expanded_task_placeholder(self):
        if self.expanded_task_entry.get().strip():
            return
        self.expanded_task_has_placeholder = True
        self.expanded_task_entry.configure(fg=MUTED)
        self.expanded_task_entry.delete(0, "end")
        self.expanded_task_entry.insert(0, self.task_placeholder)

    def clear_expanded_task_placeholder(self):
        if not self.expanded_task_has_placeholder:
            return
        self.expanded_task_has_placeholder = False
        self.expanded_task_entry.configure(fg=TEXT)
        self.expanded_task_entry.delete(0, "end")

    def get_due_string(self):
        return f"{self.due_date_var.get().strip()} {self.due_time_var.get().strip()}"

    def toggle_complete(self, task_id):
        task = self.find_task(task_id)
        if not task:
            return
        task.completed = not task.completed
        task.completed_at = datetime.now().strftime(TIME_FORMAT) if task.completed else ""
        # 后端模式
        if HAS_BACKEND and self.logged_in and task.backend_id:
            try:
                if task.completed:
                    api.complete_memo(task.backend_id)
                else:
                    api.uncomplete_memo(task.backend_id)
            except Exception:
                pass
        self.save_tasks()
        self.render()

    def delete_task(self, task_id):
        task = self.find_task(task_id)
        if HAS_BACKEND and self.logged_in and task and task.backend_id:
            try:
                api.delete_memo(task.backend_id)
            except Exception:
                pass
        self.tasks = [t for t in self.tasks if t.id != task_id]
        self.save_tasks()
        self.render()

    def clear_completed(self):
        if HAS_BACKEND and self.logged_in:
            for task in self.tasks:
                if task.completed and task.backend_id:
                    try:
                        api.delete_memo(task.backend_id)
                    except Exception:
                        pass
        for task in self.tasks:
            if task.completed:
                task.cleared = True
        self.save_tasks()
        self.render()

    def toggle_topmost(self):
        self.topmost = not self.topmost
        self.root.attributes("-topmost", self.topmost)
        self.render()

    def toggle_mode(self):
        self.save_current_size()
        self.expanded = not self.expanded
        size = self.current_mode_size()
        self.root.geometry(f"{size[0]}x{size[1]}")
        self.settings["mode"] = "expanded" if self.expanded else "normal"
        self.save_settings()
        self.render()

    def open_settings(self):
        if self.settings_panel and self.settings_panel.winfo_exists():
            self.settings_panel.lift()
            return

        panel = Toplevel(self.root)
        self.settings_panel = panel
        panel.overrideredirect(True)
        panel.attributes("-topmost", True)
        panel.attributes("-alpha", 0.98)
        panel.configure(bg=TOP)
        panel.geometry(f"320x330+{self.root.winfo_x() + self.root.winfo_width() - 330}+{self.root.winfo_y() + 62}")

        frame = tk.Frame(panel, bg=TOP, padx=16, pady=14)
        frame.pack(fill="both", expand=True)

        tk.Label(frame, text="显示选项", bg=TOP, fg=TEXT, font=("Microsoft YaHei UI", 12, "bold")).pack(anchor="w")
        tk.Label(frame, text="透明度", bg=TOP, fg=MUTED, font=("Microsoft YaHei UI", 9)).pack(anchor="w", pady=(14, 2))
        alpha_var = tk.DoubleVar(value=self.alpha * 100)
        alpha_scale = tk.Scale(
            frame,
            from_=35,
            to=100,
            orient="horizontal",
            variable=alpha_var,
            command=lambda value: self.set_alpha(float(value) / 100),
            bg=TOP,
            fg=TEXT,
            troughcolor=PANEL_2,
            activebackground=ACCENT,
            highlightthickness=0,
            bd=0,
        )
        alpha_scale.pack(fill="x")

        tk.Label(frame, text="字号大小", bg=TOP, fg=MUTED, font=("Microsoft YaHei UI", 9)).pack(anchor="w", pady=(10, 2))
        text_scale_var = tk.DoubleVar(value=self.text_scaling * 100)
        text_scale = tk.Scale(
            frame,
            from_=90,
            to=180,
            resolution=1,
            orient="horizontal",
            variable=text_scale_var,
            command=lambda value: self.set_text_scaling(float(value) / 100),
            bg=TOP,
            fg=TEXT,
            troughcolor=PANEL_2,
            activebackground=ACCENT,
            highlightthickness=0,
            bd=0,
        )
        text_scale.pack(fill="x")

        size_row = tk.Frame(frame, bg=TOP)
        size_row.pack(fill="x", pady=(10, 4))
        tk.Label(size_row, text="页面大小", bg=TOP, fg=MUTED, font=("Microsoft YaHei UI", 9)).pack(side="left")

        width_var = StringVar(value=str(self.root.winfo_width()))
        height_var = StringVar(value=str(self.root.winfo_height()))
        fields = tk.Frame(frame, bg=TOP)
        fields.pack(fill="x")
        self.settings_field(fields, "宽", width_var).pack(side="left", fill="x", expand=True, padx=(0, 6))
        self.settings_field(fields, "高", height_var).pack(side="left", fill="x", expand=True, padx=(6, 0))

        buttons = tk.Frame(frame, bg=TOP)
        buttons.pack(fill="x", pady=(14, 0))
        self.settings_button(buttons, "应用尺寸", lambda: self.apply_custom_size(width_var, height_var)).pack(side="left", expand=True, fill="x", padx=(0, 6))
        self.settings_button(buttons, "切换模式", self.toggle_mode).pack(side="left", expand=True, fill="x", padx=(6, 0))

        presets = tk.Frame(frame, bg=TOP)
        presets.pack(fill="x", pady=(10, 0))
        self.settings_button(presets, "常态尺寸", lambda: self.apply_preset_size(*NORMAL_SIZE)).pack(side="left", expand=True, fill="x", padx=(0, 6))
        self.settings_button(presets, "放大尺寸", lambda: self.apply_preset_size(*EXPANDED_SIZE)).pack(side="left", expand=True, fill="x", padx=(6, 0))

        close = tk.Button(panel, text="×", command=panel.destroy, bg=TOP, fg=TEXT, activebackground=PANEL_2, relief="flat", bd=0, font=("Microsoft YaHei UI", 12, "bold"))
        close.place(x=286, y=10, width=24, height=24)

    def settings_field(self, parent, label, variable):
        frame = tk.Frame(parent, bg=TOP)
        tk.Label(frame, text=label, bg=TOP, fg=MUTED, font=("Microsoft YaHei UI", 9)).pack(anchor="w")
        entry = tk.Entry(frame, textvariable=variable, bg=PANEL_2, fg=TEXT, insertbackground=TEXT, relief="flat", bd=0, font=("Consolas", 10))
        entry.pack(fill="x", ipady=6)
        return frame

    def settings_button(self, parent, text, command):
        return tk.Button(
            parent,
            text=text,
            command=command,
            bg=PANEL_2,
            fg=TEXT,
            activebackground=PANEL_3,
            activeforeground=TEXT,
            relief="flat",
            bd=0,
            pady=7,
            font=("Microsoft YaHei UI", 9, "bold"),
        )

    def set_alpha(self, value):
        self.alpha = max(0.35, min(1.0, value))
        self.root.attributes("-alpha", self.alpha)
        self.settings["alpha"] = self.alpha
        self.save_settings()

    def set_text_scaling(self, value):
        self.text_scaling = max(0.9, min(1.8, value))
        self.root.tk.call("tk", "scaling", self.text_scaling)
        self.settings["text_scaling"] = self.text_scaling
        self.save_settings()
        self.refresh_widget_fonts()
        self.render()

    def refresh_widget_fonts(self):
        self.task_entry.configure(font=("Microsoft YaHei UI", 16))
        self.expanded_task_entry.configure(font=("Microsoft YaHei UI", 14))
        self.date_entry.configure(font=("Consolas", 13))
        self.time_entry.configure(font=("Consolas", 13))
        self.reminder_combo.configure(font=("Microsoft YaHei UI", 13))

    def apply_custom_size(self, width_var, height_var):
        try:
            width = max(MIN_WIDTH, int(width_var.get()))
            height = max(MIN_HEIGHT, int(height_var.get()))
        except ValueError:
            messagebox.showinfo(APP_TITLE, "宽高必须是数字。")
            return
        self.root.geometry(f"{width}x{height}")
        self.save_current_size(width, height)

    def apply_preset_size(self, width, height):
        self.root.geometry(f"{width}x{height}")
        self.save_current_size(width, height)

    def current_mode_size(self):
        key = "expanded_size" if self.expanded else "normal_size"
        fallback = EXPANDED_SIZE if self.expanded else NORMAL_SIZE
        value = self.settings.get(key, fallback)
        try:
            min_width, min_height = EXPANDED_MIN_SIZE if self.expanded else (MIN_WIDTH, MIN_HEIGHT)
            width = max(min_width, int(value[0]))
            height = max(min_height, int(value[1]))
        except (TypeError, ValueError, IndexError):
            width, height = fallback
        self.settings[key] = [width, height]
        return width, height

    def save_current_size(self, width=None, height=None):
        width = width or self.root.winfo_width()
        height = height or self.root.winfo_height()
        key = "expanded_size" if self.expanded else "normal_size"
        if self.expanded:
            width = max(EXPANDED_MIN_SIZE[0], width)
            height = max(EXPANDED_MIN_SIZE[1], height)
        else:
            width = max(MIN_WIDTH, width)
            height = max(MIN_HEIGHT, height)
        self.settings[key] = [int(width), int(height)]
        self.save_settings()

    def schedule_reminder_check(self):
        self.check_reminders()
        self.root.after(30_000, self.schedule_reminder_check)

    def check_reminders(self):
        now = datetime.now()
        changed = False
        for task in self.tasks:
            if task.completed or task.reminded or not task.reminder:
                continue
            due_time = self.parse_time(task.due_at)
            if due_time and now >= due_time:
                task.reminded = True
                changed = True
                self.show_reminder(task)
        if changed:
            self.save_tasks()
            self.render()

    def show_reminder(self, task):
        self.root.bell()
        toast = Toplevel(self.root)
        toast.overrideredirect(True)
        toast.attributes("-topmost", True)
        toast.attributes("-alpha", 0.97)
        toast.configure(bg=TOP)
        toast.geometry(f"340x128+{self.root.winfo_x() + 44}+{self.root.winfo_y() + 72}")
        frame = tk.Frame(toast, bg=TOP, padx=16, pady=14)
        frame.pack(fill="both", expand=True)
        tk.Label(frame, text="任务提醒", fg=TEXT, bg=TOP, font=("Microsoft YaHei UI", 12, "bold")).pack(anchor="w")
        tk.Label(frame, text=task.content, fg=TEXT, bg=TOP, wraplength=300, justify="left", font=("Microsoft YaHei UI", 10)).pack(anchor="w", pady=(8, 0))
        tk.Label(frame, text=f"提醒时间 {self.short_due(task.due_at)}", fg=MUTED, bg=TOP, font=("Consolas", 9)).pack(anchor="w", pady=(5, 0))
        toast.after(9000, toast.destroy)

    def short_due(self, value):
        parsed = self.parse_time(value)
        if not parsed:
            return value
        return parsed.strftime("%m-%d %H:%M")

    def short_created(self, value):
        parsed = self.parse_time(value)
        if not parsed:
            return value
        return parsed.strftime("%m-%d %H:%M")

    def parse_time(self, value):
        try:
            return datetime.strptime(value, TIME_FORMAT)
        except (TypeError, ValueError):
            return None

    def find_task(self, task_id):
        for task in self.tasks:
            if task.id == task_id:
                return task
        return None

    def sample_tasks(self):
        return [
            Task("sample-1", "完善产品需求文档", "2025-05-18 09:15", "2025-05-20 10:00", "high", reminded=True),
            Task("sample-2", "准备周会汇报材料", "2025-05-19 11:30", "2025-05-20 14:00", "medium", reminded=True),
            Task("sample-3", "修复已知问题", "2025-05-17 16:40", "2025-05-19 16:30", "low", completed=True, completed_at="2025-05-17 17:25", reminded=True),
            Task("sample-4", "学习新技术栈", "2025-05-20 09:00", "2025-05-21 09:30", "medium", reminded=True),
            Task("sample-5", "整理项目资料", "2025-05-21 14:00", "2025-05-22 18:00", "low", reminded=True),
            Task("sample-6", "与团队同步进度", "2025-05-22 10:00", "2025-05-23 11:00", "high", reminded=True),
            Task("history-1", "优化登录页面样式", "2025-05-18 09:15", "2025-05-18 10:30", "high", completed=True, completed_at="2025-05-18 11:03", cleared=True, reminded=True),
            Task("history-2", "编写单元测试", "2025-05-16 10:30", "2025-05-16 11:30", "medium", completed=True, completed_at="2025-05-16 12:05", cleared=True, reminded=True),
        ]

    def load_tasks(self):
        if HAS_BACKEND and self.logged_in:
            try:
                memos = api.list_all_memos()
                self.tasks = [memo_to_task(m) for m in memos]
                return
            except Exception as e:
                print(f"Backend load failed: {e}")
        if not os.path.exists(DATA_FILE):
            self.tasks = self.sample_tasks()
            return
        try:
            with open(DATA_FILE, "r", encoding="utf-8-sig") as file:
                raw_tasks = json.load(file)
            if not raw_tasks:
                self.tasks = self.sample_tasks()
                return
            self.tasks = [self.normalize_task(item) for item in raw_tasks]
        except (OSError, json.JSONDecodeError, TypeError):
            self.tasks = self.sample_tasks()

    def normalize_task(self, item):
        data = {
            "id": item.get("id", datetime.now().strftime("%Y%m%d%H%M%S%f")),
            "content": item.get("content", ""),
            "created_at": item.get("created_at", datetime.now().strftime(TIME_FORMAT)),
            "due_at": item.get("due_at", datetime.now().strftime(TIME_FORMAT)),
            "priority": item.get("priority", "medium") if item.get("priority", "medium") in PRIORITIES else "medium",
            "completed": bool(item.get("completed", False)),
            "completed_at": item.get("completed_at", ""),
            "reminded": bool(item.get("reminded", False)),
            "reminder": bool(item.get("reminder", True)),
            "cleared": bool(item.get("cleared", False)),
        }
        return Task(**data)

    def save_tasks(self):
        if HAS_BACKEND and self.logged_in:
            return  # 后端模式下每次操作直接调 API，save 仅用于本地回退
        with open(DATA_FILE, "w", encoding="utf-8") as file:
            json.dump([asdict(task) for task in self.tasks], file, ensure_ascii=False, indent=2)

    def backend_login(self):
        """静默检查 Token 是否有效。不弹窗。"""
        token = api.load_token()
        if token:
            try:
                api.list_memos("active")
                return True
            except Exception:
                pass
        return False

    def _do_login(self):
        u = self.login_user_entry.get().strip()
        p = self.login_pass_entry.get().strip()
        if not u or not p:
            self.login_error = "请输入用户名和密码"
            self.render()
            return
        try:
            api.login(u, p)
            self.logged_in = True
            self.login_skipped = False
            self.login_error = ""
            self.load_tasks()
            self.render()
        except Exception as e:
            self.login_error = str(e)
            self.render()

    def _skip_login(self):
        self.logged_in = False
        self.login_skipped = True
        self.login_error = ""
        self.load_tasks()
        self.render()

    def _voice_input(self):
        """语音输入：录制 → ASR 转写 → 填入输入框"""
        import threading
        import sounddevice as sd
        import wave
        import tempfile

        def record_and_transcribe():
            try:
                self.voice_recording = True
                rate = 16000
                duration = 5
                self.login_error = "🎙️ 录音中..."
                self.render()

                # 录音
                recording = sd.rec(int(duration * rate), samplerate=rate,
                                   channels=1, dtype='int16')
                sd.wait()

                # 保存 WAV
                tmp = tempfile.NamedTemporaryFile(suffix=".wav", delete=False)
                wf = wave.open(tmp.name, 'wb')
                wf.setnchannels(1)
                wf.setsampwidth(2)
                wf.setframerate(rate)
                wf.writeframes(recording.tobytes())
                wf.close()

                self.login_error = "🔄 转写中..."
                self.render()

                # 上传 + 轮询
                task_id = api.upload_audio(tmp.name)
                text = api.poll_asr(task_id, timeout=30)
                if text:
                    self.task_entry.delete(0, "end")
                    self.task_entry.insert(0, text)
                    self.clear_task_placeholder()

                os.unlink(tmp.name)
                self.login_error = ""
            except Exception as e:
                self.login_error = f"语音失败: {e}"
            finally:
                self.voice_recording = False
                self.render()

        if not self.logged_in:
            self.login_error = "请先登录再使用语音输入"
            self.render()
            return

        threading.Thread(target=record_and_transcribe, daemon=True).start()

    def load_settings(self):
        defaults = {
            "alpha": 0.97,
            "mode": "normal",
            "normal_size": list(NORMAL_SIZE),
            "expanded_size": list(EXPANDED_SIZE),
            "text_scaling": TEXT_SCALING,
        }
        if not os.path.exists(SETTINGS_FILE):
            return defaults
        try:
            with open(SETTINGS_FILE, "r", encoding="utf-8-sig") as file:
                loaded = json.load(file)
            defaults.update(loaded)
        except (OSError, json.JSONDecodeError, TypeError):
            pass
        return defaults

    def save_settings(self):
        with open(SETTINGS_FILE, "w", encoding="utf-8") as file:
            json.dump(self.settings, file, ensure_ascii=False, indent=2)

    def apply_window_shape(self):
        if sys.platform != "win32":
            return
        try:
            import ctypes

            width = max(self.root.winfo_width(), 1)
            height = max(self.root.winfo_height(), 1)
            region = ctypes.windll.gdi32.CreateRoundRectRgn(0, 0, width + 1, height + 1, 34, 34)
            ctypes.windll.user32.SetWindowRgn(self.root.winfo_id(), region, True)
        except Exception:
            return

    def close(self):
        self.save_tasks()
        self.save_settings()
        self.root.destroy()


def main():
    enable_high_dpi()
    root = tk.Tk()
    root.tk.call("tk", "scaling", TEXT_SCALING)
    app = TodoApp(root)
    root.mainloop()


if __name__ == "__main__":
    main()
