import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '../store/authStore';
import LanguageSwitcher from './LanguageSwitcher';
import type { Role } from '../types';

const adminLinks = [
  { to: '/admin/dashboard', icon: 'bi-speedometer2', key: 'nav.dashboard' },
  { to: '/admin/chits', icon: 'bi-collection', key: 'nav.chits' },
  { to: '/admin/members', icon: 'bi-people', key: 'nav.members' },
  { to: '/admin/payments', icon: 'bi-cash-coin', key: 'nav.payments' },
  { to: '/admin/auctions', icon: 'bi-hammer', key: 'nav.auctions' },
  { to: '/admin/settlements', icon: 'bi-bank', key: 'nav.settlements' },
  { to: '/admin/reports/commission', icon: 'bi-graph-up', key: 'nav.commission' },
  { to: '/admin/chit-history', icon: 'bi-archive', key: 'nav.history' },
  { to: '/admin/announcements', icon: 'bi-megaphone', key: 'nav.announcements' },
  { to: '/admin/audit', icon: 'bi-journal-text', key: 'nav.audit' },
  { to: '/admin/early-exits', icon: 'bi-door-open', key: 'nav.exits' },
  { to: '/admin/risk-dashboard', icon: 'bi-exclamation-triangle', key: 'nav.risk' },
  { to: '/admin/fraud-detection', icon: 'bi-shield-exclamation', key: 'nav.fraud' },
  { to: '/admin/login-tracking', icon: 'bi-shield-lock', key: 'nav.security' },
  { to: '/admin/documents', icon: 'bi-folder2-open', key: 'nav.documents' },
  { to: '/help', icon: 'bi-question-circle', key: 'nav.help' },
];

const memberLinks = [
  { to: '/member/dashboard', icon: 'bi-house', key: 'nav.my_dashboard' },
  { to: '/member/chits', icon: 'bi-collection', key: 'nav.browse_chits' },
  { to: '/help', icon: 'bi-question-circle', key: 'nav.help' },
  { to: '/terms', icon: 'bi-file-text', key: 'nav.terms' },
];

interface Props {
  role: Role;
  children: React.ReactNode;
}

export default function Layout({ role, children }: Props) {
  const { t } = useTranslation();
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
        <Link to={role === 'ADMIN' ? '/admin/dashboard' : '/member/dashboard'} className="sidebar-brand text-decoration-none">
          <span className="brand-icon"><i className="bi bi-gem" /></span>
          <div>
            <div className="brand-title">{t('app.name')}</div>
            <div className="brand-sub">{t('app.tagline')}</div>
          </div>
        </Link>
        <nav className="sidebar-nav">
          {links.map((link) => (
            <Link
              key={link.to}
              to={link.to}
              className={`sidebar-link${location.pathname.startsWith(link.to) ? ' active' : ''}`}
              onClick={() => setOpen(false)}
            >
              <i className={`bi ${link.icon}`} />
              <span>{t(link.key)}</span>
            </Link>
          ))}
        </nav>
        <div className="sidebar-footer">
          <LanguageSwitcher />
          {role === 'MEMBER' && (
            <div className="support-block mt-2">
              <small className="text-muted d-block mb-1">{t('nav.support')}</small>
              <a href="tel:+918919508889" className="text-warning text-decoration-none small fw-bold">
                <i className="bi bi-telephone me-1" />+91 8919508889
              </a>
            </div>
          )}
          <div className="user-badge my-2">{user?.email}</div>
          <button type="button" className="btn btn-sm btn-outline-light w-100" onClick={handleLogout}>
            <i className="bi bi-box-arrow-right me-1" />{t('nav.logout')}
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
