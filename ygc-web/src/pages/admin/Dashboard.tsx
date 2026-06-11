import { useQuery } from '@tanstack/react-query';
import Layout from '../../components/Layout';
import NotificationBell from '../../components/NotificationBell';
import { adminApi } from '../../api/admin';
import { useAuthStore } from '../../store/authStore';
import { formatDate } from '../../utils/format';
import { Link } from 'react-router-dom';

export default function AdminDashboard() {
  const user = useAuthStore((s) => s.user);
  const { data, isLoading } = useQuery({ queryKey: ['admin-dashboard'], queryFn: () => adminApi.dashboard().then((r) => r.data) });

  if (isLoading) return <Layout role="ADMIN"><div className="p-4">Loading…</div></Layout>;

  const stats = [
    { label: 'Total Chits', value: data?.totalChits, icon: 'bi-collection', link: '/admin/chits' },
    { label: 'Members', value: data?.totalMembers, icon: 'bi-people', link: '/admin/members' },
    { label: 'Pending Payments', value: data?.pendingPayments, icon: 'bi-credit-card', link: '/admin/payments' },
    { label: 'Pending Settlements', value: data?.pendingSettlements, icon: 'bi-cash-stack', link: '/admin/settlements' },
    { label: 'Open Auctions', value: data?.openAuctions, icon: 'bi-hammer', link: '/admin/auctions' },
  ];

  return (
    <Layout role="ADMIN">
      <div className="topbar">
        <div>
          <h4><i className="bi bi-speedometer2 text-warning me-2" />Admin Dashboard</h4>
          <div className="sub">Welcome, {user?.fullName}</div>
        </div>
        <NotificationBell />
      </div>

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

      <div className="card">
        <div className="card-header">Recent Audit Activity</div>
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
