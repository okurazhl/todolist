// Try to get the real electron module
let electron;
// The require('electron') in Electron runtime returns the REAL API,
// but the npm package in node_modules might shadow it.
// Just try it and see.
electron = require('electron');
console.log('electron type:', typeof electron);
if (typeof electron === 'string') {
  console.error('Got path instead of API:', electron);
  process.exit(1);
}
const { app, BrowserWindow, ipcMain } = electron;
const path = require('path');

const DEV_URL = 'http://localhost:5173';
let mainWindow;

app.whenReady().then(() => {
  mainWindow = new BrowserWindow({
    width: 1100, height: 750, minWidth: 900, minHeight: 600,
    frame: false, backgroundColor: '#182334',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: false, contextIsolation: true,
    },
  });
  mainWindow.loadURL(DEV_URL);
  ipcMain.on('window-minimize', () => mainWindow.minimize());
  ipcMain.on('window-maximize', () => mainWindow.isMaximized() ? mainWindow.unmaximize() : mainWindow.maximize());
  ipcMain.on('window-close', () => mainWindow.close());

  mainWindow.webContents.on('did-finish-load', () => {
    mainWindow.webContents.insertCSS(`
      .app-layout { padding-top: 32px; }
      .sidebar { padding-top: 32px; }
      .titlebar { position: fixed; top: 0; left: 0; right: 0; height: 32px; background: #132131; z-index: 9999; display: flex; align-items: center; -webkit-app-region: drag; user-select: none; }
      .titlebar-title { color: #B8C7D6; font-size: 12px; margin-left: 12px; }
      .titlebar-controls { margin-left: auto; display: flex; height: 32px; -webkit-app-region: no-drag; }
      .titlebar-btn { width: 46px; height: 32px; border: none; background: none; color: #B8C7D6; font-size: 14px; cursor: pointer; display: flex; align-items: center; justify-content: center; }
      .titlebar-btn:hover { background: #223445; }
      .titlebar-btn.close:hover { background: #FF6F6B; color: white; }
    `);
    mainWindow.webContents.executeJavaScript(`
      const t = document.createElement('div'); t.className='titlebar';
      t.innerHTML='<span class=\"titlebar-title\">智能备忘录</span><div class=\"titlebar-controls\"><button class=\"titlebar-btn\" onclick=\"window.desktopAPI.minimize()\">━</button><button class=\"titlebar-btn\" onclick=\"window.desktopAPI.maximize()\">☐</button><button class=\"titlebar-btn close\" onclick=\"window.desktopAPI.close()\">✕</button></div>';
      document.body.prepend(t);
    `);
  });

  mainWindow.on('closed', () => { mainWindow = null; });
});

app.on('window-all-closed', () => app.quit());
