import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../features/auth/AuthStore';

const NAV_ITEMS = [
  { icon: '🏠', label: '首页', to: '/' },
  { icon: '📄', label: '全部备忘录', to: '/' },
  { icon: '🔔', label: '提醒', to: '/', badge: 3 },
  { icon: '📅', label: '今天', to: '/' },
  { icon: '⏳', label: '待办', to: '/' },
  { icon: '✅', label: '已完成', to: '/' },
  { icon: '🗑️', label: '回收站', to: '/' },
];

export function Sidebar() {
  const { username, logout } = useAuthStore();
  const navigate = useNavigate();

  const handleUserClick = () => {
    if (username) {
      if (confirm('确定要退出登录吗？')) {
        logout();
        navigate('/login');
      }
    } else {
      navigate('/login');
    }
  };

  return (
    <aside className="sidebar">
      {/* Brand */}
      <div className="sidebar-brand">
        <div className="sidebar-brand-icon">📝</div>
        <span className="sidebar-brand-name">智能备忘录</span>
      </div>

      {/* New memo button */}
      <Link to="/memo/nl" className="sidebar-btn-new">
        <span style={{ color: '#4a90d9', fontWeight: 700 }}>+</span> 新建备忘录
      </Link>

      {/* Nav menu */}
      <nav className="sidebar-nav">
        {NAV_ITEMS.map((item) => (
          <Link key={item.label} to={item.to} className="sidebar-nav-item">
            <span className="nav-icon">{item.icon}</span>
            <span>{item.label}</span>
            {item.badge && <span className="nav-badge">{item.badge}</span>}
          </Link>
        ))}
      </nav>

      {/* Bottom */}
      <div className="sidebar-bottom">
        <Link to="/categories" className="sidebar-nav-item">
          <span className="nav-icon">⚙️</span>
          <span>设置</span>
        </Link>
        <div className="sidebar-user" onClick={handleUserClick} title={username ? '点击退出登录' : '点击登录'}>
          <div className="sidebar-user-avatar">{username?.charAt(0).toUpperCase() || '?'}</div>
          <span>{username || '未登录'}</span>
          <span style={{ marginLeft: 'auto', fontSize: '0.7rem' }}>▼</span>
        </div>
      </div>
    </aside>
  );
}
