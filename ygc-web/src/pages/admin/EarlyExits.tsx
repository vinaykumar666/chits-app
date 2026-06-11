import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import Layout from '../../components/Layout';
import Topbar from '../../components/Topbar';
import AlertBanner from '../../components/AlertBanner';
import { adminApi } from '../../api/admin';
import { getErrorMessage } from '../../api/client';
import { formatCurrency, formatDate } from '../../utils/format';

export default function EarlyExitsPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [alert, setAlert] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [remarks, setRemarks] = useState<Record<number, string>>({});

  const { data, isLoading } = useQuery({
    queryKey: ['admin-early-exits'],
    queryFn: () => adminApi.earlyExits().then((r) => r.data),
  });

  const processMutation = useMutation({
    mutationFn: ({ id, approved }: { id: number; approved: boolean }) =>
      adminApi.processEarlyExit(id, approved, remarks[id]),
    onSuccess: () => {
      setAlert({ type: 'success', message: 'Exit request processed.' });
      queryClient.invalidateQueries({ queryKey: ['admin-early-exits'] });
    },
    onError: (err) => setAlert({ type: 'error', message: getErrorMessage(err) }),
  });

  if (isLoading) return <Layout role="ADMIN"><div className="p-4">{t('common.loading')}</div></Layout>;

  return (
    <Layout role="ADMIN">
      <Topbar
        title={<><i className="bi bi-door-open text-warning me-2" />{t('pages.early_exits')}</>}
        subtitle={t('pages.early_exits_sub')}
      />
      {alert && <AlertBanner type={alert.type} message={alert.message} onClose={() => setAlert(null)} />}

      <div className="card">
        <div className="table-responsive">
          <table className="table mb-0">
            <thead>
              <tr>
                <th>#</th><th>{t('common.name')}</th><th>Chit</th><th>{t('common.status')}</th>
                <th>Refund</th><th>Penalty</th><th>Reason</th><th>{t('common.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {(data?.requests ?? []).map((r) => (
                <tr key={r.id}>
                  <td>{r.id}</td>
                  <td><div className="fw-semibold">{r.memberName}</div><small className="text-muted">{r.memberEmail}</small></td>
                  <td>{r.chitName}</td>
                  <td><span className="badge bg-secondary">{r.status}</span></td>
                  <td>{formatCurrency(r.refundAmount)}</td>
                  <td>{formatCurrency(r.penaltyAmount)}</td>
                  <td className="small">{r.reason || '—'}</td>
                  <td>
                    {r.status === 'REQUESTED' || r.status === 'UNDER_REVIEW' ? (
                      <div className="d-flex flex-column gap-1" style={{ minWidth: 180 }}>
                        <input
                          className="form-control form-control-sm"
                          placeholder="Remarks"
                          value={remarks[r.id] ?? ''}
                          onChange={(e) => setRemarks({ ...remarks, [r.id]: e.target.value })}
                        />
                        <div className="d-flex gap-1">
                          <button className="btn btn-sm btn-success" onClick={() => processMutation.mutate({ id: r.id, approved: true })}>
                            {t('common.approve')}
                          </button>
                          <button className="btn btn-sm btn-danger" onClick={() => processMutation.mutate({ id: r.id, approved: false })}>
                            {t('common.reject')}
                          </button>
                        </div>
                      </div>
                    ) : (
                      <small className="text-muted">{formatDate(r.requestedAt)}</small>
                    )}
                  </td>
                </tr>
              ))}
              {!data?.requests?.length && (
                <tr><td colSpan={8} className="text-center text-muted py-4">{t('common.no_records')}</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </Layout>
  );
}
