import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import Layout from '../../components/Layout';
import Topbar from '../../components/Topbar';
import { adminApi } from '../../api/admin';
import { formatCurrency } from '../../utils/format';
import { downloadProtectedFile } from '../../utils/download';

export default function CommissionReportPage() {
  const { t } = useTranslation();
  const { data, isLoading } = useQuery({
    queryKey: ['admin-commission'],
    queryFn: () => adminApi.commissionReport().then((r) => r.data),
  });

  if (isLoading) return <Layout role="ADMIN"><div className="p-4">{t('common.loading')}</div></Layout>;

  return (
    <Layout role="ADMIN">
      <Topbar
        title={<><i className="bi bi-graph-up text-warning me-2" />{t('nav.commission')}</>}
        actions={
          <button type="button" className="btn btn-sm btn-warning" onClick={() => downloadProtectedFile('/admin/reports/commission/pdf', 'YGC_Commission.pdf')}>
            <i className="bi bi-file-pdf me-1" />{t('pages.pdf_export')}
          </button>
        }
      />
      <div className="card stat-card mb-4"><div className="card-body">
        <small className="text-muted">Total Commission</small>
        <div className="fw-bold fs-3">{formatCurrency(data?.totalCommission)}</div>
      </div></div>
      <div className="table-responsive card">
        <table className="table mb-0">
          <thead><tr><th>Chit</th><th>Source</th><th>Month</th><th>{t('common.amount')}</th></tr></thead>
          <tbody>
            {(data?.ledger ?? []).map((e) => (
              <tr key={e.id}>
                <td>{e.chitName}</td>
                <td>{e.source}</td>
                <td>{e.month?.slice(0, 10)}</td>
                <td>{formatCurrency(e.commissionAmount)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Layout>
  );
}
