import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import Layout from '../../components/Layout';
import AlertBanner from '../../components/AlertBanner';
import { adminApi } from '../../api/admin';
import { getErrorMessage } from '../../api/client';
import { formatCurrency, statusBadge } from '../../utils/format';

export default function AdminPaymentsPage() {
  const queryClient = useQueryClient();
  const [alert, setAlert] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['admin-payments'],
    queryFn: () => adminApi.payments().then((r) => r.data),
  });

  const verifyMutation = useMutation({
    mutationFn: ({ id, approved }: { id: number; approved: boolean }) => adminApi.verifyPayment(id, approved),
    onSuccess: () => { setAlert({ type: 'success', message: 'Payment updated.' }); queryClient.invalidateQueries({ queryKey: ['admin-payments'] }); },
    onError: (err) => setAlert({ type: 'error', message: getErrorMessage(err) }),
  });

  if (isLoading) return <Layout role="ADMIN"><div className="p-4">Loading…</div></Layout>;

  return (
    <Layout role="ADMIN">
      <div className="topbar"><h4><i className="bi bi-credit-card text-warning me-2" />Payments</h4></div>
      {alert && <AlertBanner type={alert.type} message={alert.message} onClose={() => setAlert(null)} />}

      <div className="card">
        <div className="card-header">Pending Verification ({data?.pendingPayments.length ?? 0})</div>
        <div className="table-responsive">
          <table className="table mb-0">
            <thead><tr><th>Member</th><th>Chit</th><th>Month</th><th>Amount</th><th>Status</th><th>Actions</th></tr></thead>
            <tbody>
              {(data?.pendingPayments ?? []).map((p) => (
                <tr key={p.id}>
                  <td>{p.memberName}</td>
                  <td>{p.chitName}</td>
                  <td>{p.monthNumber}</td>
                  <td>{formatCurrency(p.totalAmount ?? p.amount)}</td>
                  <td><span className={`badge ${statusBadge(p.status)}`}>{p.status}</span></td>
                  <td className="d-flex gap-1">
                    <button className="btn btn-sm btn-success" onClick={() => verifyMutation.mutate({ id: p.id, approved: true })}>Approve</button>
                    <button className="btn btn-sm btn-danger" onClick={() => verifyMutation.mutate({ id: p.id, approved: false })}>Reject</button>
                  </td>
                </tr>
              ))}
              {!data?.pendingPayments.length && <tr><td colSpan={6} className="text-center text-muted py-4">No pending payments</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </Layout>
  );
}
