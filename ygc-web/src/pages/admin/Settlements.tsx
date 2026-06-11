import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import Layout from '../../components/Layout';
import AlertBanner from '../../components/AlertBanner';
import { adminApi } from '../../api/admin';
import { getErrorMessage } from '../../api/client';
import { formatCurrency, statusBadge } from '../../utils/format';

export default function AdminSettlementsPage() {
  const queryClient = useQueryClient();
  const [alert, setAlert] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['admin-settlements'],
    queryFn: () => adminApi.settlements().then((r) => r.data),
  });

  const processMutation = useMutation({
    mutationFn: ({ id, approved }: { id: number; approved: boolean }) => adminApi.processSettlement(id, approved),
    onSuccess: () => { setAlert({ type: 'success', message: 'Settlement processed.' }); queryClient.invalidateQueries({ queryKey: ['admin-settlements'] }); },
    onError: (err) => setAlert({ type: 'error', message: getErrorMessage(err) }),
  });

  if (isLoading) return <Layout role="ADMIN"><div className="p-4">Loading…</div></Layout>;

  return (
    <Layout role="ADMIN">
      <div className="topbar"><h4><i className="bi bi-cash-stack text-warning me-2" />Settlements</h4></div>
      {alert && <AlertBanner type={alert.type} message={alert.message} onClose={() => setAlert(null)} />}

      <div className="table-responsive card">
        <table className="table mb-0">
          <thead><tr><th>Member</th><th>Chit</th><th>Type</th><th>Amount</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>
            {(data?.pendingSettlements ?? []).map((s) => (
              <tr key={s.id}>
                <td>{s.memberName}</td>
                <td>{s.chitName}</td>
                <td>{s.type}</td>
                <td>{formatCurrency(s.finalSettlementAmount)}</td>
                <td><span className={`badge ${statusBadge(s.status)}`}>{s.status}</span></td>
                <td className="d-flex gap-1">
                  <button className="btn btn-sm btn-success" onClick={() => processMutation.mutate({ id: s.id, approved: true })}>Approve</button>
                  <button className="btn btn-sm btn-danger" onClick={() => processMutation.mutate({ id: s.id, approved: false })}>Reject</button>
                </td>
              </tr>
            ))}
            {!data?.pendingSettlements.length && <tr><td colSpan={6} className="text-center text-muted py-4">No pending settlements</td></tr>}
          </tbody>
        </table>
      </div>
    </Layout>
  );
}
