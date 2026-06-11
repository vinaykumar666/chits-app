import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import Layout from '../../components/Layout';
import Topbar from '../../components/Topbar';
import AlertBanner from '../../components/AlertBanner';
import { adminApi } from '../../api/admin';
import { getErrorMessage } from '../../api/client';
import { formatDate } from '../../utils/format';

export default function LoginTrackingPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [alert, setAlert] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['admin-login-tracking'],
    queryFn: () => adminApi.loginTracking().then((r) => r.data),
  });

  const aadhaarMutation = useMutation({
    mutationFn: (id: number) => adminApi.toggleAadhaar(id),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin-login-tracking'] }); setAlert({ type: 'success', message: 'Aadhaar status updated.' }); },
    onError: (err) => setAlert({ type: 'error', message: getErrorMessage(err) }),
  });

  const unlockMutation = useMutation({
    mutationFn: (id: number) => adminApi.resetLoginCounter(id),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin-login-tracking'] }); setAlert({ type: 'success', message: 'Account unlocked.' }); },
    onError: (err) => setAlert({ type: 'error', message: getErrorMessage(err) }),
  });

  if (isLoading) return <Layout role="ADMIN"><div className="p-4">{t('common.loading')}</div></Layout>;

  const stats = [
    { label: 'Total Logins', value: data?.totalLogins, icon: 'bi-box-arrow-in-right' },
    { label: 'Success Rate', value: `${data?.successRate ?? 0}%`, icon: 'bi-check-circle' },
    { label: 'Unique IPs', value: data?.uniqueIPs, icon: 'bi-globe' },
    { label: 'Aadhaar Compliance', value: `${data?.aadhaarCompliancePct ?? 0}%`, icon: 'bi-person-badge' },
    { label: 'Suspicious', value: data?.suspiciousCount, icon: 'bi-shield-exclamation' },
  ];

  return (
    <Layout role="ADMIN">
      <Topbar
        title={<><i className="bi bi-shield-lock text-warning me-2" />{t('pages.security')}</>}
        subtitle={t('pages.security_sub')}
      />
      {alert && <AlertBanner type={alert.type} message={alert.message} onClose={() => setAlert(null)} />}

      <div className="row g-3 mb-4">
        {stats.map((s) => (
          <div key={s.label} className="col-md-4 col-6">
            <div className="card stat-card h-100"><div className="card-body d-flex align-items-center gap-3">
              <i className={`bi ${s.icon} fs-3 text-warning`} />
              <div><div className="fw-bold fs-4">{s.value ?? 0}</div><small className="text-muted">{s.label}</small></div>
            </div></div>
          </div>
        ))}
      </div>

      <div className="row g-4">
        <div className="col-lg-6">
          <div className="card">
            <div className="card-header">Locked Accounts ({data?.lockedAccounts.length ?? 0})</div>
            <div className="table-responsive">
              <table className="table mb-0 table-sm">
                <tbody>
                  {(data?.lockedAccounts ?? []).map((u) => (
                    <tr key={u.id}>
                      <td>{u.fullName}<br /><small>{u.email}</small></td>
                      <td><button className="btn btn-sm btn-warning" onClick={() => unlockMutation.mutate(u.id)}>Unlock</button></td>
                    </tr>
                  ))}
                  {!data?.lockedAccounts?.length && <tr><td className="text-muted">{t('common.no_records')}</td></tr>}
                </tbody>
              </table>
            </div>
          </div>
        </div>
        <div className="col-lg-6">
          <div className="card">
            <div className="card-header">Aadhaar — Verified {data?.aadhaarVerified} / Pending {data?.aadhaarPending}</div>
            <div className="table-responsive" style={{ maxHeight: 280, overflow: 'auto' }}>
              <table className="table mb-0 table-sm">
                <tbody>
                  {(data?.allMembers ?? []).slice(0, 20).map((u) => (
                    <tr key={u.id}>
                      <td>{u.fullName}</td>
                      <td><span className={`badge bg-${u.aadhaarVerified ? 'success' : 'secondary'}`}>{u.aadhaarVerified ? 'Verified' : 'Pending'}</span></td>
                      <td><button className="btn btn-sm btn-outline-light" onClick={() => aadhaarMutation.mutate(u.id!)}>Toggle</button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
        <div className="col-12">
          <div className="card">
            <div className="card-header">Recent Login Activity</div>
            <div className="table-responsive">
              <table className="table mb-0">
                <thead><tr><th>User</th><th>IP</th><th>Device</th><th>Status</th><th>Time</th></tr></thead>
                <tbody>
                  {(data?.recentLogins ?? []).slice(0, 25).map((l) => (
                    <tr key={l.id}>
                      <td>{l.userName || l.userEmail}</td>
                      <td><code>{l.ipAddress}</code></td>
                      <td className="small text-truncate" style={{ maxWidth: 200 }}>{l.userAgent}</td>
                      <td><span className={`badge bg-${l.success ? 'success' : 'danger'}`}>{l.success ? 'Success' : l.failureReason || 'Failed'}</span></td>
                      <td className="small">{formatDate(l.loginAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
}
