import { useQuery } from '@tanstack/react-query';
import Layout from '../../components/Layout';
import { adminApi } from '../../api/admin';
import { formatDate, statusBadge } from '../../utils/format';

export default function ChitHistoryPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['admin-chit-history'],
    queryFn: () => adminApi.chitHistory().then((r) => r.data),
  });

  if (isLoading) return <Layout role="ADMIN"><div className="p-4">Loading…</div></Layout>;

  return (
    <Layout role="ADMIN">
      <div className="topbar"><h4><i className="bi bi-archive text-warning me-2" />Chit History</h4></div>
      <div className="row g-3 mb-4">
        <div className="col-md-4"><div className="card stat-card"><div className="card-body">Deleted: <strong>{data?.deletedCount}</strong></div></div></div>
        <div className="col-md-4"><div className="card stat-card"><div className="card-body">Completed: <strong>{data?.completedCount}</strong></div></div></div>
        <div className="col-md-4"><div className="card stat-card"><div className="card-body">Cancelled: <strong>{data?.cancelledCount}</strong></div></div></div>
      </div>
      <div className="table-responsive card">
        <table className="table mb-0">
          <thead><tr><th>Chit</th><th>Status</th><th>Reason</th><th>Closed</th></tr></thead>
          <tbody>
            {(data?.histories ?? []).map((h) => (
              <tr key={h.id}>
                <td>{h.chitName}</td>
                <td><span className={`badge ${statusBadge(h.finalStatus)}`}>{h.finalStatus}</span></td>
                <td className="small">{h.closingReason || '—'}</td>
                <td>{formatDate(h.closedAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Layout>
  );
}
