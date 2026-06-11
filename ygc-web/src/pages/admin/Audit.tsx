import { useQuery } from '@tanstack/react-query';
import Layout from '../../components/Layout';
import { adminApi } from '../../api/admin';
import { formatDate } from '../../utils/format';

export default function AuditPage() {
  const { data = [], isLoading } = useQuery({
    queryKey: ['admin-audit'],
    queryFn: () => adminApi.audit().then((r) => r.data),
  });

  if (isLoading) return <Layout role="ADMIN"><div className="p-4">Loading…</div></Layout>;

  return (
    <Layout role="ADMIN">
      <div className="topbar"><h4><i className="bi bi-journal-text text-warning me-2" />Audit Log</h4></div>
      <div className="table-responsive card">
        <table className="table mb-0 table-sm">
          <thead><tr><th>Time</th><th>User</th><th>Action</th><th>Entity</th><th>Description</th></tr></thead>
          <tbody>
            {data.map((a) => (
              <tr key={a.id}>
                <td>{formatDate(a.timestamp)}</td>
                <td>{a.userName || a.userEmail}</td>
                <td><code>{a.action}</code></td>
                <td>{a.entityType} #{a.entityId}</td>
                <td className="small">{a.description}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Layout>
  );
}
