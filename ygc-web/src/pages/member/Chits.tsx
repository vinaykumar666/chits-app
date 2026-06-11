import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import Layout from '../../components/Layout';
import AlertBanner from '../../components/AlertBanner';
import { memberApi } from '../../api/member';
import { getErrorMessage } from '../../api/client';
import { formatCurrency, statusBadge } from '../../utils/format';
import { Link } from 'react-router-dom';

export default function MemberChitsPage() {
  const queryClient = useQueryClient();
  const [alert, setAlert] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [joinId, setJoinId] = useState<number | null>(null);
  const [agreements, setAgreements] = useState({ agreementRead: false, termsAccepted: false, infoProcessingAuthorized: false });

  const { data, isLoading } = useQuery({
    queryKey: ['member-chits'],
    queryFn: () => memberApi.chits().then((r) => r.data),
  });

  const joinMutation = useMutation({
    mutationFn: (id: number) => memberApi.joinChit(id, agreements),
    onSuccess: () => {
      setAlert({ type: 'success', message: 'Join request submitted! Awaiting admin approval.' });
      setJoinId(null);
      queryClient.invalidateQueries({ queryKey: ['member-chits'] });
    },
    onError: (err) => setAlert({ type: 'error', message: getErrorMessage(err) }),
  });

  if (isLoading) return <Layout role="MEMBER"><div className="p-4">Loading…</div></Layout>;

  return (
    <Layout role="MEMBER">
      <div className="topbar"><h4><i className="bi bi-collection text-warning me-2" />Available Chits</h4></div>
      {alert && <AlertBanner type={alert.type} message={alert.message} onClose={() => setAlert(null)} />}

      <div className="row g-3">
        {(data?.chits ?? []).map((chit) => {
          const myStatus = data?.myChitStatus?.[chit.id];
          const isMember = !!myStatus;
          return (
            <div key={chit.id} className="col-md-6 col-lg-4">
              <div className="card h-100">
                <div className="card-body">
                  <div className="d-flex justify-content-between align-items-start mb-2">
                    <h5 className="card-title mb-0">{chit.name}</h5>
                    <span className={`badge ${statusBadge(chit.status)}`}>{chit.status}</span>
                  </div>
                  <p className="text-muted small">{chit.description || 'No description'}</p>
                  <div className="small mb-3">
                    <div>Monthly: <strong>{formatCurrency(chit.monthlyAmount)}</strong></div>
                    <div>Members: {chit.totalMembers} · Duration: {chit.durationMonths} mo</div>
                  </div>
                  {isMember ? (
                    <button className="btn btn-secondary w-100" disabled>
                      {myStatus === 'ACTIVE' ? 'Already a Member' : myStatus === 'PENDING' ? 'Request Pending' : myStatus}
                    </button>
                  ) : joinId === chit.id ? (
                    <div>
                      {(['agreementRead', 'termsAccepted', 'infoProcessingAuthorized'] as const).map((key) => (
                        <div className="form-check small" key={key}>
                          <input className="form-check-input" type="checkbox" id={`${chit.id}-${key}`}
                            checked={agreements[key]} onChange={(e) => setAgreements({ ...agreements, [key]: e.target.checked })} />
                          <label className="form-check-label" htmlFor={`${chit.id}-${key}`}>
                            {key === 'agreementRead' ? 'I have read the agreement' :
                             key === 'termsAccepted' ? 'I accept terms & conditions' : 'I authorize info processing'}
                          </label>
                        </div>
                      ))}
                      <div className="d-flex gap-2 mt-2">
                        <button className="btn btn-warning btn-sm flex-fill" disabled={joinMutation.isPending}
                          onClick={() => joinMutation.mutate(chit.id)}>Submit Request</button>
                        <button className="btn btn-outline-secondary btn-sm" onClick={() => setJoinId(null)}>Cancel</button>
                      </div>
                    </div>
                  ) : (
                    <button className="btn btn-warning w-100" onClick={() => { setJoinId(chit.id); setAgreements({ agreementRead: false, termsAccepted: false, infoProcessingAuthorized: false }); }}>
                      Request to Join
                    </button>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>
      <p className="mt-3"><Link to="/member/dashboard">← Back to dashboard</Link></p>
    </Layout>
  );
}
