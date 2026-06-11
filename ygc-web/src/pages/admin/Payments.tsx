import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import Layout from '../../components/Layout';
import Topbar from '../../components/Topbar';
import AlertBanner from '../../components/AlertBanner';
import { adminApi } from '../../api/admin';
import { getErrorMessage, getStoredAuth } from '../../api/client';
import { formatCurrency, statusBadge } from '../../utils/format';

function ScreenshotModal({ paymentId, onClose }: { paymentId: number; onClose: () => void }) {
  const [src, setSrc] = useState<string | null>(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    const auth = getStoredAuth();
    fetch(adminApi.paymentScreenshotUrl(paymentId), {
      headers: auth?.accessToken ? { Authorization: `Bearer ${auth.accessToken}` } : {},
    })
      .then((r) => (r.ok ? r.blob() : Promise.reject()))
      .then((blob) => setSrc(URL.createObjectURL(blob)))
      .catch(() => setError(true));
    return () => { if (src) URL.revokeObjectURL(src); };
  }, [paymentId]);

  return (
    <div className="modal d-block" style={{ background: 'rgba(0,0,0,.6)' }} onClick={onClose}>
      <div className="modal-dialog modal-lg modal-dialog-centered" onClick={(e) => e.stopPropagation()}>
        <div className="modal-content screenshot-modal">
          <div className="modal-header">
            <h5 className="modal-title">Payment Screenshot</h5>
            <button type="button" className="btn-close" onClick={onClose} />
          </div>
          <div className="modal-body text-center">
            {error && <p className="text-muted">Screenshot not available</p>}
            {src && <img src={src} alt="Payment proof" />}
            {!src && !error && <div className="spinner-border text-warning" />}
          </div>
        </div>
      </div>
    </div>
  );
}

export default function AdminPaymentsPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [alert, setAlert] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [viewScreenshot, setViewScreenshot] = useState<number | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['admin-payments'],
    queryFn: () => adminApi.payments().then((r) => r.data),
  });

  const verifyMutation = useMutation({
    mutationFn: ({ id, approved }: { id: number; approved: boolean }) => adminApi.verifyPayment(id, approved),
    onSuccess: () => { setAlert({ type: 'success', message: 'Payment updated.' }); queryClient.invalidateQueries({ queryKey: ['admin-payments'] }); },
    onError: (err) => setAlert({ type: 'error', message: getErrorMessage(err) }),
  });

  if (isLoading) return <Layout role="ADMIN"><div className="p-4">{t('common.loading')}</div></Layout>;

  const renderRow = (p: NonNullable<typeof data>['pendingPayments'][number], pending: boolean) => (
    <tr key={p.id}>
      <td>{p.memberName}</td>
      <td>{p.chitName}</td>
      <td>{p.monthNumber}</td>
      <td>{formatCurrency(p.totalAmount ?? p.amount)}</td>
      <td><span className={`badge ${statusBadge(p.status)}`}>{p.status}</span></td>
      <td>
        <button type="button" className="btn btn-sm btn-outline-secondary me-1" onClick={() => setViewScreenshot(p.id)}>
          <i className="bi bi-image" />
        </button>
        {pending && (
          <>
            <button className="btn btn-sm btn-success me-1" onClick={() => verifyMutation.mutate({ id: p.id, approved: true })}>{t('common.approve')}</button>
            <button className="btn btn-sm btn-danger" onClick={() => verifyMutation.mutate({ id: p.id, approved: false })}>{t('common.reject')}</button>
          </>
        )}
      </td>
    </tr>
  );

  return (
    <Layout role="ADMIN">
      <Topbar
        title={<><i className="bi bi-credit-card text-warning me-2" />{t('pages.payments')}</>}
        subtitle={t('pages.payments_sub')}
      />
      {alert && <AlertBanner type={alert.type} message={alert.message} onClose={() => setAlert(null)} />}
      {viewScreenshot && <ScreenshotModal paymentId={viewScreenshot} onClose={() => setViewScreenshot(null)} />}

      <div className="card mb-4">
        <div className="card-header">Pending Verification ({data?.pendingPayments.length ?? 0})</div>
        <div className="table-responsive">
          <table className="table mb-0">
            <thead><tr><th>Member</th><th>Chit</th><th>Month</th><th>{t('common.amount')}</th><th>{t('common.status')}</th><th>{t('common.actions')}</th></tr></thead>
            <tbody>
              {(data?.pendingPayments ?? []).map((p) => renderRow(p, true))}
              {!data?.pendingPayments.length && <tr><td colSpan={6} className="text-center text-muted py-4">{t('common.no_records')}</td></tr>}
            </tbody>
          </table>
        </div>
      </div>

      <div className="card">
        <div className="card-header">All Payments</div>
        <div className="table-responsive">
          <table className="table mb-0 table-sm">
            <thead><tr><th>Member</th><th>Chit</th><th>Month</th><th>{t('common.amount')}</th><th>{t('common.status')}</th><th></th></tr></thead>
            <tbody>
              {(data?.allPayments ?? []).slice(0, 50).map((p) => renderRow(p, false))}
            </tbody>
          </table>
        </div>
      </div>
    </Layout>
  );
}
