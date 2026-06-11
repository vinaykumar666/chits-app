import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import type { Role } from '../types';

const adminLinks = [
  { to: '/admin/dashboard', icon: 'bi-speedometer2', label: 'Dashboard' },
  { to: '/admin/chits', icon: 'bi-collection', label: 'Chits' },
  { to: '/admin/members', icon: 'bi-people', label: 'Members' },
  { to: '/admin/payments', icon: 'bi-credit-card', label: 'Payments' },
  { to: '/admin/auctions', icon: 'bi-hammer', label: 'Auctions' },
  { to: '/admin/settlements', icon: 'bi-cash-stack', label: 'Settlements' },
  { to: '/admin/reports/commission', icon: 'bi-graph-up', label: 'Commission' },
  { to: '/admin/announcements', icon: 'bi-megaphone', label: 'Announcements' },
  { to: '/admin/audit', icon: 'bi-journal-text', label: 'Audit Log' },
  { to: '/admin/chit-history', icon: 'bi-archive', label: 'Chit History' },
];

const memberLinks = [
  { to: '/member/dashboard', icon: 'bi-house', label: 'Dashboard' },
  { to: '/member/chits', icon: 'bi-collection', label: 'Browse Chits' },
];

interface Props {
  role: Role;
  children: React.ReactNode;
}

export default function Layout({ role, children }: Props) {
  const { user, logout } = useAuthStore();
  const location = useLocation();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const links = role === 'ADMIN' ? adminLinks : memberLinks;
  const sidebarClass = role === 'ADMIN' ? 'sidebar' : 'sidebar member-sidebar';

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="app-shell">
      <aside className={`${sidebarClass}${open ? ' open' : ''}`}>
        <div className="sidebar-brand">
          <span className="brand-icon"><i className="bi bi-gem" /></span>
          <div>
            <div className="brand-title">YGC Internal</div>
            <div className="brand-sub">Chit Management</div>
          </div>
        </div>
        <nav className="sidebar-nav">
          {links.map((link) => (
            <Link
              key={link.to}
              to={link.to}
              className={`sidebar-link${location.pathname.startsWith(link.to) ? ' active' : ''}`}
              onClick={() => setOpen(false)}
            >
              <i className={`bi ${link.icon}`} />
              <span>{link.label}</span>
            </Link>
          ))}
        </nav>
        <div className="sidebar-footer">
          <div className="user-badge mb-2">{user?.email}</div>
          <button type="button" className="btn btn-sm btn-outline-light w-100" onClick={handleLogout}>
            <i className="bi bi-box-arrow-right me-1" />Logout
          </button>
        </div>
      </aside>

      <button type="button" className="mobile-toggle" onClick={() => setOpen(!open)} aria-label="Menu">
        <i className="bi bi-list" />
      </button>

      <main className="main-content">{children}</main>
    </div>
  );
}
