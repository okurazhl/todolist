@echo off
:: 智能备忘录桌面端启动脚本
:: 使用 Chrome App Mode 模拟原生桌面体验

set URL=http://localhost:5173

:: 查找 Chrome
set CHROME=
if exist "C:\Program Files\Google\Chrome\Application\chrome.exe" set CHROME=C:\Program Files\Google\Chrome\Application\chrome.exe
if exist "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe" set CHROME=C:\Program Files (x86)\Google\Chrome\Application\chrome.exe
if exist "%LOCALAPPDATA%\Google\Chrome\Application\chrome.exe" set CHROME=%LOCALAPPDATA%\Google\Chrome\Application\chrome.exe

if "%CHROME%"=="" (
    echo Chrome not found. Opening in default browser.
    start %URL%
    goto :end
)

echo Starting SmartMemo Desktop...
start "" "%CHROME%" --app=%URL% --window-size=1100,750

:end
