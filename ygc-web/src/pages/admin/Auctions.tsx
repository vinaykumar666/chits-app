import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import Layout from '../../components/Layout';
import AlertBanner from '../../components/AlertBanner';
import { adminApi } from '../../api/admin';
import { getErrorMessage } from '../../api/client';
import { statusBadge } from '../../utils/format';

export default function AdminAuctionsPage() {
  const queryClient = useQueryClient();
  const [alert, setAlert] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [form, setForm] = useState({ chitId: '', monthNumber: '1', auctionDate: new Date().toISOString().slice(0, 10) });

  const { data, isLoading } = useQuery({
    queryKey: ['admin-auctions'],
    queryFn: () => adminApi.auctions().then((r) => r.data),
  });

  const createMutation = useMutation({
    mutationFn: () => adminApi.createAuction({ chitId: Number(form.chitId), monthNumber: Number(form.monthNumber), auctionDate: form.auctionDate }),
    onSuccess: () => { setAlert({ type: 'success', message: 'Auction created.' }); queryClient.invalidateQueries({ queryKey: ['admin-auctions'] }); },
    onError: (err) => setAlert({ type: 'error', message: getErrorMessage(err) }),
  });

  const action = (fn: (id: number) => Promise<unknown>, id: number, msg: string) => {
    fn(id).then(() => { setAlert({ type: 'success', message: msg }); queryClient.invalidateQueries({ queryKey: ['admin-auctions'] }); })
      .catch((err) => setAlert({ type: 'error', message: getErrorMessage(err) }));
  };

  if (isLoading) return <Layout role="ADMIN"><div className="p-4">Loading…</div></Layout>;

  return (
    <Layout role="ADMIN">
      <div className="topbar"><h4><i className="bi bi-hammer text-warning me-2" />Auctions</h4></div>
      {alert && <AlertBanner type={alert.type} message={alert.message} onClose={() => setAlert(null)} />}

      <div className="card mb-4"><div className="card-header">Create Auction</div><div className="card-body row g-2 align-items-end">
        <div className="col-md-4">
          <label className="form-label">Chit</label>
          <select className="form-select" value={form.chitId} onChange={(e) => setForm({ ...form, chitId: e.target.value })}>
            <option value="">Select chit</option>
            {(data?.chits ?? []).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </div>
        <div className="col-md-2"><label className="form-label">Month</label><input type="number" className="form-control" value={form.monthNumber} onChange={(e) => setForm({ ...form, monthNumber: e.target.value })} /></div>
        <div className="col-md-3"><label className="form-label">Date</label><input type="date" className="form-control" value={form.auctionDate} onChange={(e) => setForm({ ...form, auctionDate: e.target.value })} /></div>
        <div className="col-md-3"><button className="btn btn-warning w-100" disabled={!form.chitId} onClick={() => createMutation.mutate()}>Announce</button></div>
      </div></div>

      <div className="table-responsive card">
        <table className="table mb-0">
          <thead><tr><th>Chit</th><th>Month</th><th>Date</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>
            {(data?.allAuctions ?? []).map((a) => (
              <tr key={a.id}>
                <td>{a.chitName}</td>
                <td>{a.monthNumber}</td>
                <td>{a.auctionDate}</td>
                <td><span className={`badge ${statusBadge(a.status)}`}>{a.status}</span></td>
                <td className="d-flex gap-1 flex-wrap">
                  {a.status === 'ANNOUNCED' && <button className="btn btn-sm btn-warning" onClick={() => action(adminApi.openAuction, a.id, 'Auction opened')}>Open</button>}
                  {a.status === 'OPEN' && <button className="btn btn-sm btn-secondary" onClick={() => action(adminApi.closeAuction, a.id, 'Auction closed')}>Close</button>}
                  {a.status === 'CLOSED' && !a.payoutReleased && <button className="btn btn-sm btn-success" onClick={() => action(adminApi.releasePayout, a.id, 'Payout released')}>Release Payout</button>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Layout>
  );
}
