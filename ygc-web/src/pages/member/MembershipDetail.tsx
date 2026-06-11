import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import Layout from '../../components/Layout';
import AlertBanner from '../../components/AlertBanner';
import { memberApi } from '../../api/member';
import { getErrorMessage } from '../../api/client';
import { formatCurrency, formatDate, statusBadge } from '../../utils/format';

export default function MembershipDetailPage() {
  const { id } = useParams<{ id: string }>();
  const membershipId = Number(id);
  const queryClient = useQueryClient();
  const [alert, setAlert] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [monthNumber, setMonthNumber] = useState(1);
  const [screenshot, setScreenshot] = useState<File | undefined>();
  const [bidAmount, setBidAmount] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['membership', membershipId],
    queryFn: () => memberApi.membership(membershipId).then((r) => r.data),
    enabled: !!membershipId,
  });

  const paymentMutation = useMutation({
    mutationFn: () => memberApi.submitPayment(membershipId, monthNumber, screenshot),
    onSuccess: () => {
      setAlert({ type: 'success', message: 'Payment submitted!' });
      queryClient.invalidateQueries({ queryKey: ['membership', membershipId] });
    },
    onError: (err) => setAlert({ type: 'error', message: getErrorMessage(err) }),
  });

  const bidMutation = useMutation({
    mutationFn: (auctionId: number) => memberApi.placeBid(auctionId, Number(bidAmount)),
    onSuccess: () => setAlert({ type: 'success', message: 'Bid placed successfully!' }),
    onError: (err) => setAlert({ type: 'error', message: getErrorMessage(err) }),
  });

  const exitMutation = useMutation({
    mutationFn: () => memberApi.requestExit(membershipId),
    onSuccess: () => {
      setAlert({ type: 'success', message: 'Early exit request submitted.' });
      queryClient.invalidateQueries({ queryKey: ['membership', membershipId] });
    },
    onError: (err) => setAlert({ type: 'error', message: getErrorMessage(err) }),
  });

  if (isLoading || !data) return <Layout role="MEMBER"><div className="p-4">Loading…</div></Layout>;

  const openAuction = data.auctions.find((a) => a.status === 'OPEN');

  return (
    <Layout role="MEMBER">
      <div className="topbar">
        <div>
          <h4>{data.membership.chit.name}</h4>
          <div className="sub">Membership #{data.membership.id} · <span className={`badge ${statusBadge(data.membership.status)}`}>{data.membership.status}</span></div>
        </div>
      </div>
      {alert && <AlertBanner type={alert.type} message={alert.message} onClose={() => setAlert(null)} />}

      <div className="row g-4 mb-4">
        <div className="col-md-4"><div className="card stat-card"><div className="card-body">
          <div className="text-muted small">Total Paid</div>
          <div className="fw-bold fs-4">{formatCurrency(data.totalPaid)}</div>
        </div></div></div>
        <div className="col-md-4"><div className="card stat-card"><div className="card-body">
          <div className="text-muted small">Monthly Amount</div>
          <div className="fw-bold fs-4">{formatCurrency(data.membership.chit.monthlyAmount)}</div>
        </div></div></div>
      </div>

      <div className="card mb-4">
        <div className="card-header">Submit Payment</div>
        <div className="card-body row g-3 align-items-end">
          <div className="col-md-3">
            <label className="form-label">Month #</label>
            <input type="number" min={1} className="form-control" value={monthNumber} onChange={(e) => setMonthNumber(Number(e.target.value))} />
          </div>
          <div className="col-md-5">
            <label className="form-label">Payment Screenshot</label>
            <input type="file" className="form-control" accept="image/*" onChange={(e) => setScreenshot(e.target.files?.[0])} />
          </div>
          <div className="col-md-4">
            <button className="btn btn-warning w-100" disabled={paymentMutation.isPending} onClick={() => paymentMutation.mutate()}>Submit Payment</button>
          </div>
        </div>
      </div>

      {data.hasOpenAuction && openAuction && !data.membership.hasWonAuction && (
        <div className="card mb-4">
          <div className="card-header">Place Bid — Month {openAuction.monthNumber}</div>
          <div className="card-body d-flex gap-2 align-items-end flex-wrap">
            <div>
              <label className="form-label">Bid Amount (₹)</label>
              <input type="number" className="form-control" value={bidAmount} onChange={(e) => setBidAmount(e.target.value)} />
            </div>
            <button className="btn btn-warning" disabled={!bidAmount || bidMutation.isPending}
              onClick={() => bidMutation.mutate(openAuction.id)}>Place Bid</button>
          </div>
        </div>
      )}

      <div className="card mb-4">
        <div className="card-header d-flex justify-content-between">
          <span>Payments</span>
          {data.membership.status === 'ACTIVE' && (
            <button className="btn btn-sm btn-outline-danger" onClick={() => exitMutation.mutate()} disabled={exitMutation.isPending}>
              Request Early Exit
            </button>
          )}
        </div>
        <div className="table-responsive">
          <table className="table mb-0">
            <thead><tr><th>Month</th><th>Amount</th><th>Status</th><th>Date</th></tr></thead>
            <tbody>
              {data.payments.map((p) => (
                <tr key={p.id}>
                  <td>{p.monthNumber}</td>
                  <td>{formatCurrency(p.totalAmount ?? p.amount)}</td>
                  <td><span className={`badge ${statusBadge(p.status)}`}>{p.status}</span></td>
                  <td>{formatDate(p.createdAt)}</td>
                </tr>
              ))}
              {!data.payments.length && <tr><td colSpan={4} className="text-center text-muted py-3">No payments yet</td></tr>}
            </tbody>
          </table>
        </div>
      </div>

      <Link to="/member/dashboard">← Back to dashboard</Link>
    </Layout>
  );
}
