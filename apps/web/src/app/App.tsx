import { BrowserRouter, Routes, Route, Link as RouterLink, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../features/auth/AuthStore';
import { ProtectedRoute } from '../features/auth/ProtectedRoute';
import { LoginPage } from '../features/auth/LoginPage';
import { RegisterPage } from '../features/auth/RegisterPage';
import { MemoListPage } from '../features/memo/MemoListPage';
import { MemoEditorPage } from '../features/memo/MemoEditorPage';
import { MemoDetailPage } from '../features/memo/MemoDetailPage';
import { TagManagePage } from '../features/tag/TagManagePage';
import { CategoryManagePage } from '../features/category/CategoryManagePage';
import { HealthPage } from '../features/health/HealthPage';

function NavBar() {
  const { isLoggedIn, username, logout } = useAuthStore();
  const navigate = useNavigate();

  return (
    <header className="app-header">
      <h1><RouterLink to="/">📝 智能备忘录</RouterLink></h1>
      <nav>
        {isLoggedIn ? (
          <>
            <RouterLink to="/">备忘录</RouterLink>
            <RouterLink to="/tags">标签</RouterLink>
            <RouterLink to="/categories">分类</RouterLink>
            <span className="nav-user">👤 {username}</span>
            <button onClick={() => { logout(); navigate('/login'); }}>退出</button>
          </>
        ) : (
          <>
            <RouterLink to="/login">登录</RouterLink>
            <RouterLink to="/health">系统状态</RouterLink>
          </>
        )}
      </nav>
    </header>
  );
}

export function App() {
  return (
    <BrowserRouter>
      <div className="app">
        <NavBar />
        <main className="app-main">
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/health" element={<HealthPage />} />
            <Route path="/" element={<ProtectedRoute><MemoListPage /></ProtectedRoute>} />
            <Route path="/memo/new" element={<ProtectedRoute><MemoEditorPage /></ProtectedRoute>} />
            <Route path="/memo/:id" element={<ProtectedRoute><MemoDetailPage /></ProtectedRoute>} />
            <Route path="/memo/:id/edit" element={<ProtectedRoute><MemoEditorPage /></ProtectedRoute>} />
            <Route path="/tags" element={<ProtectedRoute><TagManagePage /></ProtectedRoute>} />
            <Route path="/categories" element={<ProtectedRoute><CategoryManagePage /></ProtectedRoute>} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}
