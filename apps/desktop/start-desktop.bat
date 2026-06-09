@echo off
cd /d "%~dp0"

:: 检查是否已在运行
tasklist /FI "WINDOWTITLE eq *智能备忘录*" 2>NUL | find /I "python" >NUL
if %ERRORLEVEL%==0 (
    echo 桌面端已在运行中，按 Alt+Tab 切换窗口
    exit /b
)

:: 用独立 venv 启动（避免 Windows Store Python 文件冲突）
.venv\Scripts\python.exe desktop-app.py
