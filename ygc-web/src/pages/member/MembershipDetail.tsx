import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import Layout from '../../components/Layout';
import Topbar from '../../components/Topbar';
import AlertBanner from '../../components/AlertBanner';
import { memberApi } from '../../api/member';
import { getErrorMessage } from '../../api/client';
import { formatCurrency, formatDate, statusBadge } from '../../utils/format';
import { downloadProtectedFile } from '../../utils/download';

export default function MembershipDetailPage() {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const membershipId = Number(id);
  const queryClient = useQueryClient();
  const [alert, setAlert] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [monthNumber, setMonthNumber] = useState(1);
  const [screenshot, setScreenshot] = useState<File | undefined>();
  const [bidAmount, setBidAmount] = useState('');
  const [calcBid, setCalcBid] = useState('');
  const [calcMonth, setCalcMonth] = useState(1);

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

  const ackMutation = useMutation({
    mutationFn: (settlementId: number) => memberApi.acknowledgeSettlement(settlementId),
    onSuccess: () => {
      setAlert({ type: 'success', message: 'Settlement acknowledged.' });
      queryClient.invalidateQueries({ queryKey: ['membership', membershipId] });
    },
    onError: (err) => setAlert({ type: 'error', message: getErrorMessage(err) }),
  });

  const calcQuery = useQuery({
    queryKey: ['bid-calc', data?.membership.chit.id, calcBid, calcMonth],
    queryFn: () =>
      memberApi
        .bidCalculator(data!.membership.chit.id, calcBid ? Number(calcBid) : undefined, calcMonth)
        .then((r) => r.data),
    enabled: !!data?.membership.chit.id && !!calcBid,
  });

  const pendingSettlement = data?.mySettlements?.find(
    (s) => s.status === 'APPROVED' && !s.userAcknowledged,
  );

  if (isLoading || !data) return <Layout role="MEMBER"><div className="p-4">Loading…</div></Layout>;

  const openAuction = data.auctions.find((a) => a.status === 'OPEN');

  return (
    <Layout role="MEMBER">
      <Topbar
        title={data.membership.chit.name}
        subtitle={<>Membership #{data.membership.id} · <span className={`badge ${statusBadge(data.membership.status)}`}>{data.membership.status}</span></>}
        actions={
          <button
            type="button"
            className="btn btn-sm btn-outline-warning"
            onClick={() => downloadProtectedFile(`/member/reports/memberships/${membershipId}/payments/pdf`, `payments-${membershipId}.pdf`)}
          >
            <i className="bi bi-file-pdf me-1" />{t('pages.pdf_export')}
          </button>
        }
      />
      {alert && <AlertBanner type={alert.type} message={alert.message} onClose={() => setAlert(null)} />}

      {pendingSettlement && (
        <div className="card mb-4 border-warning">
          <div className="card-body d-flex flex-wrap align-items-center justify-content-between gap-3">
            <div>
              <div className="fw-bold text-warning"><i className="bi bi-cash-stack me-2" />{t('pages.settlement_ready')}</div>
              <div className="small">Amount: {formatCurrency(pendingSettlement.finalSettlementAmount)}</div>
            </div>
            <button className="btn btn-warning" disabled={ackMutation.isPending} onClick={() => ackMutation.mutate(pendingSettlement.id)}>
              {t('pages.settlement_ack')}
            </button>
          </div>
        </div>
      )}

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

      {data.bidRecommendations && (
        <div className="card mb-4">
          <div className="card-header">{t('pages.bid_calculator')}</div>
          <div className="card-body">
            <div className="row g-3 align-items-end mb-3">
              <div className="col-md-4">
                <label className="form-label">Month #</label>
                <input type="number" min={1} className="form-control" value={calcMonth} onChange={(e) => setCalcMonth(Number(e.target.value))} />
              </div>
              <div className="col-md-4">
                <label className="form-label">Bid Amount (₹)</label>
                <input type="number" className="form-control" value={calcBid} onChange={(e) => setCalcBid(e.target.value)} />
              </div>
            </div>
            {calcQuery.data && (
              <div className="row g-2 small">
                {Object.entries(calcQuery.data).map(([k, v]) => (
                  <div key={k} className="col-md-4"><span className="text-muted">{k}:</span> <strong>{String(v)}</strong></div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

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
