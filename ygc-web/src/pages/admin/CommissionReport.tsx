import { useQuery } from '@tanstack/react-query';
import Layout from '../../components/Layout';
import { adminApi } from '../../api/admin';
import { formatCurrency } from '../../utils/format';

export default function CommissionReportPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['admin-commission'],
    queryFn: () => adminApi.commissionReport().then((r) => r.data),
  });

  if (isLoading) return <Layout role="ADMIN"><div className="p-4">Loading…</div></Layout>;

  return (
    <Layout role="ADMIN">
      <div className="topbar"><h4><i className="bi bi-graph-up text-warning me-2" />Commission Report</h4></div>
      <div className="card stat-card mb-4"><div className="card-body">
        <small className="text-muted">Total Commission</small>
        <div className="fw-bold fs-3">{formatCurrency(data?.totalCommission)}</div>
      </div></div>
      <div className="table-responsive card">
        <table className="table mb-0">
          <thead><tr><th>Chit</th><th>Source</th><th>Month</th><th>Amount</th></tr></thead>
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
