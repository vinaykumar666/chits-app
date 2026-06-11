import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import Layout from '../../components/Layout';
import Topbar from '../../components/Topbar';
import { adminApi } from '../../api/admin';

export default function FraudDetectionPage() {
  const { t } = useTranslation();
  const { data, isLoading } = useQuery({
    queryKey: ['admin-fraud'],
    queryFn: () => adminApi.fraudDetection().then((r) => r.data),
  });

  if (isLoading) return <Layout role="ADMIN"><div className="p-4">{t('common.loading')}</div></Layout>;

  return (
    <Layout role="ADMIN">
      <Topbar
        title={<><i className="bi bi-shield-exclamation text-warning me-2" />{t('pages.fraud')}</>}
        subtitle={t('pages.fraud_sub')}
      />

      <div className="row g-4">
        <div className="col-md-6">
          <div className="card">
            <div className="card-header text-danger"><i className="bi bi-person-badge me-2" />Duplicate Aadhaar</div>
            <ul className="list-group list-group-flush">
              {(data?.duplicateAadhaar ?? []).map((d, i) => (
                <li key={i} className="list-group-item small">
                  <strong>{d.value || d.aadhaar}</strong> — {d.emails || d.users} ({d.count || '2+'} accounts)
                </li>
              ))}
              {!data?.duplicateAadhaar?.length && <li className="list-group-item text-muted">{t('common.no_records')}</li>}
            </ul>
          </div>
        </div>
        <div className="col-md-6">
          <div className="card">
            <div className="card-header text-danger"><i className="bi bi-telephone me-2" />Duplicate Phone</div>
            <ul className="list-group list-group-flush">
              {(data?.duplicatePhone ?? []).map((d, i) => (
                <li key={i} className="list-group-item small">
                  <strong>{d.value || d.phone}</strong> — {d.emails || d.users}
                </li>
              ))}
              {!data?.duplicatePhone?.length && <li className="list-group-item text-muted">{t('common.no_records')}</li>}
            </ul>
          </div>
        </div>
        <div className="col-md-6">
          <div className="card">
            <div className="card-header"><i className="bi bi-exclamation-octagon me-2" />Watchlist</div>
            <div className="table-responsive">
              <table className="table mb-0">
                <tbody>
                  {(data?.watchlistMembers ?? []).map((u) => (
                    <tr key={u.id}>
                      <td>{u.fullName}<br /><small>{u.email}</small></td>
                      <td><Link to={`/admin/members/${u.id}/profile`} className="btn btn-sm btn-outline-warning">{t('common.view')}</Link></td>
                    </tr>
                  ))}
                  {!data?.watchlistMembers?.length && <tr><td className="text-muted">{t('common.no_records')}</td></tr>}
                </tbody>
              </table>
            </div>
          </div>
        </div>
        <div className="col-md-6">
          <div className="card">
            <div className="card-header"><i className="bi bi-graph-down-arrow me-2" />High Risk (score ≥ 50)</div>
            <div className="table-responsive">
              <table className="table mb-0">
                <tbody>
                  {(data?.highRiskMembers ?? []).slice(0, 10).map((a) => (
                    <tr key={a.user.id}>
                      <td>{a.user.fullName}</td>
                      <td><span className="badge bg-danger">{a.riskScore}</span></td>
                      <td>{a.tier}</td>
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
