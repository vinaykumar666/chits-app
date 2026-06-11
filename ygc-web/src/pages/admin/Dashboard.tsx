import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import Layout from '../../components/Layout';
import Topbar from '../../components/Topbar';
import { adminApi } from '../../api/admin';
import { useAuthStore } from '../../store/authStore';
import { formatDate } from '../../utils/format';

export default function AdminDashboard() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const { data, isLoading } = useQuery({ queryKey: ['admin-dashboard'], queryFn: () => adminApi.dashboard().then((r) => r.data) });

  if (isLoading) return <Layout role="ADMIN"><div className="p-4">{t('common.loading')}</div></Layout>;

  const stats = [
    { label: t('dash.total_chits'), value: data?.totalChits, icon: 'bi-collection', link: '/admin/chits' },
    { label: t('dash.total_members'), value: data?.totalMembers, icon: 'bi-people', link: '/admin/members' },
    { label: t('dash.pending_pay'), value: data?.pendingPayments, icon: 'bi-credit-card', link: '/admin/payments' },
    { label: t('dash.pending_settlements'), value: data?.pendingSettlements, icon: 'bi-cash-stack', link: '/admin/settlements' },
    { label: t('dash.open_auctions'), value: data?.openAuctions, icon: 'bi-hammer', link: '/admin/auctions' },
  ];

  const quickActions = [
    { to: '/admin/chits', icon: 'bi-plus-circle', label: 'Create Chit' },
    { to: '/admin/payments', icon: 'bi-cash-coin', label: 'Verify Payments' },
    { to: '/admin/auctions', icon: 'bi-hammer', label: 'Auctions' },
    { to: '/admin/early-exits', icon: 'bi-door-open', label: 'Early Exits' },
    { to: '/admin/risk-dashboard', icon: 'bi-exclamation-triangle', label: 'Risk' },
    { to: '/admin/reports/commission', icon: 'bi-file-pdf', label: 'Reports' },
  ];

  return (
    <Layout role="ADMIN">
      <Topbar
        title={<><i className="bi bi-speedometer2 text-warning me-2" />{t('dash.admin_title')}</>}
        subtitle={`${t('dash.welcome')}, ${user?.fullName}`}
      />

      <div className="row g-3 mb-4">
        {stats.map((s) => (
          <div key={s.label} className="col-md-4 col-6">
            <Link to={s.link} className="text-decoration-none">
              <div className="card stat-card h-100"><div className="card-body d-flex align-items-center gap-3">
                <i className={`bi ${s.icon} fs-3 text-warning`} />
                <div><div className="fw-bold fs-4">{s.value ?? 0}</div><small className="text-muted">{s.label}</small></div>
              </div></div>
            </Link>
          </div>
        ))}
      </div>

      <div className="card mb-4">
        <div className="card-header">{t('dash.quick_actions')}</div>
        <div className="card-body quick-action-grid">
          {quickActions.map((a) => (
            <Link key={a.to} to={a.to} className="quick-action-btn">
              <i className={`bi ${a.icon}`} />
              <span className="small fw-semibold text-center">{a.label}</span>
            </Link>
          ))}
        </div>
      </div>

      <div className="card">
        <div className="card-header">{t('dash.recent_activity')}</div>
        <div className="table-responsive">
          <table className="table mb-0">
            <thead><tr><th>Time</th><th>User</th><th>Action</th><th>Description</th></tr></thead>
            <tbody>
              {(data?.recentAudits ?? []).map((a) => (
                <tr key={a.id}>
                  <td className="small">{formatDate(a.timestamp)}</td>
                  <td>{a.userName || a.userEmail}</td>
                  <td><code>{a.action}</code></td>
                  <td className="small">{a.description}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </Layout>
  );
}
