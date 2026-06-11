import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import Layout from '../../components/Layout';
import Topbar from '../../components/Topbar';
import { adminApi } from '../../api/admin';
import { formatDate } from '../../utils/format';

function riskBadge(tier: string) {
  const map: Record<string, string> = { LOW: 'success', MEDIUM: 'warning', HIGH: 'orange', CRITICAL: 'danger' };
  return map[tier] ?? 'secondary';
}

export default function RiskDashboardPage() {
  const { t } = useTranslation();
  const { data, isLoading } = useQuery({
    queryKey: ['admin-risk'],
    queryFn: () => adminApi.riskDashboard().then((r) => r.data),
  });

  if (isLoading) return <Layout role="ADMIN"><div className="p-4">{t('common.loading')}</div></Layout>;

  return (
    <Layout role="ADMIN">
      <Topbar
        title={<><i className="bi bi-exclamation-triangle text-warning me-2" />{t('pages.risk')}</>}
        subtitle={t('pages.risk_sub')}
      />

      <div className="row g-4 mb-4">
        <div className="col-lg-7">
          <div className="card h-100">
            <div className="card-header">High Risk Members ({data?.alerts.length ?? 0})</div>
            <div className="table-responsive">
              <table className="table mb-0">
                <thead><tr><th>Member</th><th>Score</th><th>Tier</th><th></th></tr></thead>
                <tbody>
                  {(data?.alerts ?? []).map((a) => (
                    <tr key={a.user.id}>
                      <td><div className="fw-semibold">{a.user.fullName}</div><small>{a.user.email}</small></td>
                      <td><span className="fw-bold">{a.riskScore}</span></td>
                      <td><span className={`badge bg-${riskBadge(a.tier)}`}>{a.tier}</span></td>
                      <td><Link to={`/admin/members/${a.user.id}/profile`} className="btn btn-sm btn-outline-warning">{t('common.view')}</Link></td>
                    </tr>
                  ))}
                  {!data?.alerts?.length && <tr><td colSpan={4} className="text-center text-muted py-4">{t('common.no_records')}</td></tr>}
                </tbody>
              </table>
            </div>
          </div>
        </div>
        <div className="col-lg-5">
          <div className="card h-100">
            <div className="card-header">Recent Login Activity</div>
            <div className="table-responsive">
              <table className="table mb-0 table-sm">
                <thead><tr><th>User</th><th>IP</th><th>Status</th><th>Time</th></tr></thead>
                <tbody>
                  {(data?.recentLogins ?? []).slice(0, 12).map((l) => (
                    <tr key={l.id}>
                      <td className="small">{l.userName || l.userEmail}</td>
                      <td className="small"><code>{l.ipAddress}</code></td>
                      <td><span className={`badge bg-${l.success ? 'success' : 'danger'}`}>{l.success ? 'OK' : 'Fail'}</span></td>
                      <td className="small">{formatDate(l.loginAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
}
