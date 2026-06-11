import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import Layout from '../../components/Layout';
import Topbar from '../../components/Topbar';
import { adminApi } from '../../api/admin';
import { formatCurrency, statusBadge } from '../../utils/format';

export default function DocumentsPage() {
  const { t } = useTranslation();
  const { data, isLoading } = useQuery({
    queryKey: ['admin-documents'],
    queryFn: () => adminApi.documents().then((r) => r.data),
  });

  if (isLoading) return <Layout role="ADMIN"><div className="p-4">{t('common.loading')}</div></Layout>;

  return (
    <Layout role="ADMIN">
      <Topbar
        title={<><i className="bi bi-folder2-open text-warning me-2" />{t('pages.documents')}</>}
        subtitle={t('pages.documents_sub')}
      />

      <div className="row g-4">
        <div className="col-lg-6">
          <div className="card">
            <div className="card-header"><i className="bi bi-file-earmark-pdf me-2" />Agreements ({data?.agreements.length ?? 0})</div>
            <div className="table-responsive">
              <table className="table mb-0">
                <thead><tr><th>Member</th><th>Chit</th><th>Agreement #</th></tr></thead>
                <tbody>
                  {(data?.agreements ?? []).map((a) => (
                    <tr key={a.membershipId}>
                      <td>{a.memberName}</td>
                      <td>{a.chitName}</td>
                      <td><code>{a.agreementNumber || '—'}</code></td>
                    </tr>
                  ))}
                  {!data?.agreements?.length && <tr><td colSpan={3} className="text-muted text-center py-3">{t('common.no_records')}</td></tr>}
                </tbody>
              </table>
            </div>
          </div>
        </div>
        <div className="col-lg-6">
          <div className="card">
            <div className="card-header"><i className="bi bi-award me-2" />Certificates ({data?.certificates.length ?? 0})</div>
            <div className="table-responsive">
              <table className="table mb-0">
                <thead><tr><th>Member</th><th>Chit</th></tr></thead>
                <tbody>
                  {(data?.certificates ?? []).map((c) => (
                    <tr key={c.membershipId}><td>{c.memberName}</td><td>{c.chitName}</td></tr>
                  ))}
                  {!data?.certificates?.length && <tr><td colSpan={2} className="text-muted text-center py-3">{t('common.no_records')}</td></tr>}
                </tbody>
              </table>
            </div>
          </div>
        </div>
        <div className="col-12">
          <div className="card">
            <div className="card-header"><i className="bi bi-bank me-2" />Settlements</div>
            <div className="table-responsive">
              <table className="table mb-0">
                <thead><tr><th>Member</th><th>Chit</th><th>Amount</th><th>Status</th></tr></thead>
                <tbody>
                  {(data?.settlements ?? []).slice(0, 30).map((s) => (
                    <tr key={s.id}>
                      <td>{s.memberName}</td>
                      <td>{s.chitName}</td>
                      <td>{formatCurrency(s.finalSettlementAmount)}</td>
                      <td><span className={`badge ${statusBadge(s.status)}`}>{s.status}</span></td>
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
