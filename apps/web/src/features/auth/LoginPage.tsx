import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { login } from '../../shared/api/auth';
import { useAuthStore } from './AuthStore';

const ACCOUNTS_KEY = 'saved_accounts';
const MAX_SAVED = 5;

interface SavedAccount {
  username: string;
  savedAt: number;
}

function loadAccounts(): SavedAccount[] {
  try {
    const raw = localStorage.getItem(ACCOUNTS_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function saveAccount(username: string) {
  const accounts = loadAccounts().filter((a) => a.username !== username);
  accounts.unshift({ username, savedAt: Date.now() });
  if (accounts.length > MAX_SAVED) accounts.pop();
  localStorage.setItem(ACCOUNTS_KEY, JSON.stringify(accounts));
}

function removeAccount(username: string) {
  const accounts = loadAccounts().filter((a) => a.username !== username);
  localStorage.setItem(ACCOUNTS_KEY, JSON.stringify(accounts));
}

export function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [savedAccounts, setSavedAccounts] = useState<SavedAccount[]>(loadAccounts);
  const authLogin = useAuthStore((s) => s.login);
  const navigate = useNavigate();

  const refreshAccounts = () => setSavedAccounts(loadAccounts());

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const result = await login(username, password);
      authLogin(result.accessToken, result.refreshToken, username);
      saveAccount(username);
      navigate('/');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : '登录失败';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleSwitchAccount = (account: SavedAccount) => {
    setUsername(account.username);
    setPassword('');
    setError('');
  };

  const handleRemoveAccount = (e: React.MouseEvent, username: string) => {
    e.stopPropagation();
    removeAccount(username);
    refreshAccounts();
  };

  return (
    <div className="auth-page">
      <h2>登录</h2>

      {/* 切换账户 */}
      {savedAccounts.length > 0 && (
        <div className="saved-accounts">
          <div className="saved-accounts-label">切换账户</div>
          <div className="saved-accounts-list">
            {savedAccounts.map((a) => (
              <div
                key={a.username}
                className={`saved-account-chip${a.username === username ? ' active' : ''}`}
                onClick={() => handleSwitchAccount(a)}
              >
                <span className="saved-account-avatar">
                  {a.username.charAt(0).toUpperCase()}
                </span>
                <span className="saved-account-name">{a.username}</span>
                <span
                  className="saved-account-remove"
                  onClick={(e) => handleRemoveAccount(e, a.username)}
                  title="移除"
                >
                  ✕
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <label>
          用户名
          <input value={username} onChange={(e) => setUsername(e.target.value)} required minLength={3} />
        </label>
        <label>
          密码
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={8} />
        </label>
        {error && <p className="form-error">{error}</p>}
        <button type="submit" disabled={loading}>{loading ? '登录中...' : '登录'}</button>
      </form>
      <p className="auth-link">没有账号？<Link to="/register">注册</Link></p>
    </div>
  );
}
