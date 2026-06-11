import { Link, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import Layout from '../../components/Layout';
import Topbar from '../../components/Topbar';
import { adminApi } from '../../api/admin';
import { formatDate, statusBadge } from '../../utils/format';

export default function MemberProfilePage() {
  const { id } = useParams<{ id: string }>();
  const { t } = useTranslation();
  const memberId = Number(id);

  const { data, isLoading } = useQuery({
    queryKey: ['admin-member-profile', memberId],
    queryFn: () => adminApi.memberProfile(memberId).then((r) => r.data),
    enabled: !!memberId,
  });

  if (isLoading || !data) return <Layout role="ADMIN"><div className="p-4">{t('common.loading')}</div></Layout>;

  const m = data.member;

  return (
    <Layout role="ADMIN">
      <Topbar
        title={<><i className="bi bi-person-badge text-warning me-2" />{t('pages.member_profile')}</>}
        subtitle={m.fullName}
        actions={<Link to="/admin/members" className="btn btn-sm btn-outline-light">{t('common.back')}</Link>}
      />

      <div className="row g-3 mb-4">
        {[
          { label: 'Payment Score', value: `${data.paymentScore}%`, icon: 'bi-graph-up' },
          { label: 'Risk Score', value: data.riskScore, icon: 'bi-exclamation-triangle' },
          { label: 'Trust Rating', value: data.trustRating, icon: 'bi-shield-check' },
          { label: 'Active Chits', value: data.activeChits, icon: 'bi-collection' },
          { label: 'On-time Payments', value: data.onTimePayments, icon: 'bi-check2-circle' },
          { label: 'Overdue', value: data.overduePayments, icon: 'bi-clock-history' },
        ].map((s) => (
          <div key={s.label} className="col-md-4 col-6">
            <div className="card stat-card h-100"><div className="card-body d-flex align-items-center gap-3">
              <i className={`bi ${s.icon} fs-4 text-warning`} />
              <div><div className="fw-bold">{s.value}</div><small className="text-muted">{s.label}</small></div>
            </div></div>
          </div>
        ))}
      </div>

      <div className="card mb-4">
        <div className="card-header">Contact</div>
        <div className="card-body row g-2">
          <div className="col-md-4"><strong>{t('common.email')}:</strong> {m.email}</div>
          <div className="col-md-4"><strong>{t('common.phone')}:</strong> {m.phone || '—'}</div>
          <div className="col-md-4"><strong>Aadhaar:</strong> <span className={`badge bg-${m.aadhaarVerified ? 'success' : 'secondary'}`}>{m.aadhaarVerified ? 'Verified' : 'Pending'}</span></div>
        </div>
      </div>

      <div className="card mb-4">
        <div className="card-header">Memberships</div>
        <div className="table-responsive">
          <table className="table mb-0">
            <thead><tr><th>Chit</th><th>Status</th><th>Joined</th></tr></thead>
            <tbody>
              {data.memberships.map((ms) => (
                <tr key={ms.id}>
                  <td>{ms.chit.name}</td>
                  <td><span className={`badge ${statusBadge(ms.status)}`}>{ms.status}</span></td>
                  <td className="small">{formatDate(ms.joinedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="card">
        <div className="card-header">Login History</div>
        <div className="table-responsive">
          <table className="table mb-0 table-sm">
            <thead><tr><th>IP</th><th>Status</th><th>Time</th></tr></thead>
            <tbody>
              {data.loginHistory.map((l) => (
                <tr key={l.id}>
                  <td><code>{l.ipAddress}</code></td>
                  <td><span className={`badge bg-${l.success ? 'success' : 'danger'}`}>{l.success ? 'OK' : 'Fail'}</span></td>
                  <td>{formatDate(l.loginAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </Layout>
  );
}
