import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { ProtectedRoute } from '../features/auth/ProtectedRoute';
import { LoginPage } from '../features/auth/LoginPage';
import { RegisterPage } from '../features/auth/RegisterPage';
import { MemoListPage } from '../features/memo/MemoListPage';
import { MemoEditorPage } from '../features/memo/MemoEditorPage';
import { MemoDetailPage } from '../features/memo/MemoDetailPage';
import { TagManagePage } from '../features/tag/TagManagePage';
import { CategoryManagePage } from '../features/category/CategoryManagePage';
import { HealthPage } from '../features/health/HealthPage';
import { NaturalLanguageMemoPage } from '../features/nl-memo/NaturalLanguageMemoPage';
import { AppLayout } from './AppLayout';

export function App() {
  return (
    <BrowserRouter>
      <AppLayout>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/health" element={<HealthPage />} />
          <Route path="/" element={<ProtectedRoute><MemoListPage /></ProtectedRoute>} />
          <Route path="/memo/new" element={<ProtectedRoute><MemoEditorPage /></ProtectedRoute>} />
          <Route path="/memo/nl" element={<ProtectedRoute><NaturalLanguageMemoPage /></ProtectedRoute>} />
          <Route path="/memo/:id" element={<ProtectedRoute><MemoDetailPage /></ProtectedRoute>} />
          <Route path="/memo/:id/edit" element={<ProtectedRoute><MemoEditorPage /></ProtectedRoute>} />
          <Route path="/tags" element={<ProtectedRoute><TagManagePage /></ProtectedRoute>} />
          <Route path="/categories" element={<ProtectedRoute><CategoryManagePage /></ProtectedRoute>} />
        </Routes>
      </AppLayout>
    </BrowserRouter>
  );
}
