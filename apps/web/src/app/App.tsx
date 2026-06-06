import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import { HealthPage } from '../features/health/HealthPage';

export function App() {
  return (
    <BrowserRouter>
      <div className="app">
        <header className="app-header">
          <h1>📝 智能备忘录</h1>
          <nav>
            <Link to="/">首页</Link>
            <Link to="/health">系统状态</Link>
          </nav>
        </header>
        <main className="app-main">
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/health" element={<HealthPage />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}

function HomePage() {
  return (
    <div className="home-page">
      <h2>欢迎使用智能备忘录系统</h2>
      <p>MVP 第一阶段：基础工程搭建</p>
      <div className="status-card">
        <h3>开发进度</h3>
        <ul>
          <li>✅ 项目骨架 &amp; Monorepo</li>
          <li>✅ Docker Compose 基础设施</li>
          <li>✅ API Gateway 服务</li>
          <li>✅ Web 端骨架</li>
          <li>⬜ 用户 &amp; 鉴权模块</li>
          <li>⬜ 备忘录 CRUD</li>
        </ul>
      </div>
    </div>
  );
}
