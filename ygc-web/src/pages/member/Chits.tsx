import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import Layout from '../../components/Layout';
import AlertBanner from '../../components/AlertBanner';
import { memberApi } from '../../api/member';
import { getErrorMessage } from '../../api/client';
import { formatCurrency, statusBadge } from '../../utils/format';
import {
  canJoinChit,
  getMyChitStatus,
  membershipStatusBadgeClass,
  membershipStatusLabel,
} from '../../utils/chitMembership';
import { Link } from 'react-router-dom';

const MIN_JOIN_REASON = 15;

export default function MemberChitsPage() {
  const queryClient = useQueryClient();
  const [alert, setAlert] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [search, setSearch] = useState('');
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [joinReason, setJoinReason] = useState('');
  const [agreements, setAgreements] = useState({
    agreementRead: false,
    termsAccepted: false,
    infoProcessingAuthorized: false,
  });

  const { data, isLoading } = useQuery({
    queryKey: ['member-chits'],
    queryFn: () => memberApi.chits().then((r) => r.data),
  });

  const filteredChits = useMemo(() => {
    const q = search.trim().toLowerCase();
    const chits = data?.chits ?? [];
    if (!q) return chits;
    return chits.filter(
      (c) =>
        c.name.toLowerCase().includes(q) ||
        (c.description ?? '').toLowerCase().includes(q),
    );
  }, [data?.chits, search]);

  const joinMutation = useMutation({
    mutationFn: (id: number) =>
      memberApi.joinChit(id, { ...agreements, joinReason: joinReason.trim() }),
    onSuccess: () => {
      setAlert({ type: 'success', message: 'Join request submitted! Awaiting admin approval.' });
      setExpandedId(null);
      setJoinReason('');
      setAgreements({ agreementRead: false, termsAccepted: false, infoProcessingAuthorized: false });
      queryClient.invalidateQueries({ queryKey: ['member-chits'] });
      queryClient.invalidateQueries({ queryKey: ['member-dashboard'] });
    },
    onError: (err) => setAlert({ type: 'error', message: getErrorMessage(err) }),
  });

  const resetJoinForm = () => {
    setExpandedId(null);
    setJoinReason('');
    setAgreements({ agreementRead: false, termsAccepted: false, infoProcessingAuthorized: false });
  };

  const allAgreementsChecked =
    agreements.agreementRead && agreements.termsAccepted && agreements.infoProcessingAuthorized;
  const joinReasonValid = joinReason.trim().length >= MIN_JOIN_REASON;

  if (isLoading) {
    return (
      <Layout role="MEMBER">
        <div className="p-4">Loading…</div>
      </Layout>
    );
  }

  return (
    <Layout role="MEMBER">
      <div className="topbar">
        <div>
          <h4>
            <i className="bi bi-collection text-warning me-2" />
            Browse Chits
          </h4>
          <div className="sub">Find open groups and submit your join request</div>
        </div>
      </div>

      {alert && <AlertBanner type={alert.type} message={alert.message} onClose={() => setAlert(null)} />}

      <div className="card border-0 shadow-sm mb-4 chit-search-bar">
        <div className="card-body py-3">
          <div className="row g-2 align-items-center">
            <div className="col-md-8">
              <div className="input-group">
                <span className="input-group-text bg-white">
                  <i className="bi bi-search" />
                </span>
                <input
                  type="search"
                  className="form-control"
                  placeholder="Search by chit name or description…"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  autoComplete="off"
                />
              </div>
            </div>
            <div className="col-md-4 text-md-end">
              <span className="badge bg-light text-dark border">
                {filteredChits.length} chit{filteredChits.length === 1 ? '' : 's'} available
              </span>
            </div>
          </div>
        </div>
      </div>

      {!filteredChits.length ? (
        <div className="card border-0 shadow-sm text-center py-5">
          <div className="card-body text-muted">
            <i className="bi bi-inbox fs-1 d-block mb-3" />
            {search ? 'No chits match your search.' : 'No chits are open for joining right now.'}
          </div>
        </div>
      ) : (
        <div className="row g-4">
          {filteredChits.map((chit) => {
            const myStatus = getMyChitStatus(data?.myChitStatus, chit.id);
            const isMember = myStatus && !canJoinChit(myStatus);
            const isExpanded = expandedId === chit.id;

            return (
              <div key={chit.id} className="col-md-6 col-lg-4">
                <div className="card h-100 chit-card border-0 shadow-sm">
                  <div className="chit-card-accent" />
                  <div className="card-body d-flex flex-column">
                    <div className="d-flex justify-content-between align-items-start mb-2 gap-2">
                      <h5 className="fw-bold mb-0">{chit.name}</h5>
                      {myStatus && myStatus !== 'EXITED' ? (
                        <span className={`badge flex-shrink-0 ${membershipStatusBadgeClass(myStatus)}`}>
                          {myStatus === 'ACTIVE' && <i className="bi bi-patch-check-fill me-1" />}
                          {myStatus === 'PENDING' && <i className="bi bi-hourglass-split me-1" />}
                          {membershipStatusLabel(myStatus)}
                        </span>
                      ) : (
                        <span className={`badge ${statusBadge(chit.status)} flex-shrink-0`}>{chit.status}</span>
                      )}
                    </div>

                    <p className="text-muted small mb-3">
                      {chit.description || 'Regular monthly savings group with auction-based payouts.'}
                    </p>

                    <div className="row g-2 mb-3">
                      <div className="col-6">
                        <div className="chit-stat-box chit-stat-highlight">
                          <div className="fw-bold text-success">{formatCurrency(chit.monthlyAmount)}</div>
                          <small className="text-muted">Monthly</small>
                        </div>
                      </div>
                      <div className="col-6">
                        <div className="chit-stat-box">
                          <div className="fw-bold">{chit.durationMonths} mo</div>
                          <small className="text-muted">Duration</small>
                        </div>
                      </div>
                      <div className="col-6">
                        <div className="chit-stat-box">
                          <div className="fw-bold">{chit.totalMembers}</div>
                          <small className="text-muted">Max Members</small>
                        </div>
                      </div>
                      <div className="col-6">
                        <div className="chit-stat-box">
                          <div className="fw-bold text-warning">
                            {chit.totalChitValue != null ? formatCurrency(chit.totalChitValue) : '—'}
                          </div>
                          <small className="text-muted">Total Value</small>
                        </div>
                      </div>
                    </div>

                    <div className="text-muted small mb-3">
                      <i className="bi bi-calendar3 me-1" />
                      Starts: <strong>{chit.startDate}</strong>
                    </div>

                    <div className="mt-auto">
                      {isMember ? (
                        <button type="button" className="btn btn-secondary w-100 fw-bold" disabled>
                          <i className="bi bi-person-check me-1" />
                          {membershipStatusLabel(myStatus!)}
                        </button>
                      ) : isExpanded ? (
                        <div className="join-panel">
                          <label className="form-label small fw-semibold">
                            Why do you want to join this chit? <span className="text-danger">*</span>
                          </label>
                          <textarea
                            className="form-control form-control-sm mb-1"
                            rows={3}
                            maxLength={2000}
                            placeholder="Explain your savings goals, why this chit fits you, and how you plan to participate…"
                            value={joinReason}
                            onChange={(e) => setJoinReason(e.target.value)}
                          />
                          <div className={`form-text mb-2 ${joinReason.trim().length > 0 && !joinReasonValid ? 'text-danger' : ''}`}>
                            Minimum {MIN_JOIN_REASON} characters — helps admin review your request.
                            {joinReason.trim().length > 0 && ` (${joinReason.trim().length}/${MIN_JOIN_REASON})`}
                          </div>

                          {(['agreementRead', 'termsAccepted', 'infoProcessingAuthorized'] as const).map((key) => (
                            <div className="form-check small mb-1" key={key}>
                              <input
                                className="form-check-input"
                                type="checkbox"
                                id={`${chit.id}-${key}`}
                                checked={agreements[key]}
                                onChange={(e) => setAgreements({ ...agreements, [key]: e.target.checked })}
                              />
                              <label className="form-check-label" htmlFor={`${chit.id}-${key}`}>
                                {key === 'agreementRead'
                                  ? 'I have read the chit terms'
                                  : key === 'termsAccepted'
                                    ? 'I accept all terms & conditions'
                                    : 'I authorize info processing (KYC)'}
                              </label>
                            </div>
                          ))}

                          <div className="d-flex gap-2 mt-3">
                            <button
                              type="button"
                              className="btn btn-warning btn-sm flex-fill fw-bold"
                              disabled={joinMutation.isPending || !allAgreementsChecked || !joinReasonValid}
                              onClick={() => joinMutation.mutate(chit.id)}
                            >
                              {joinMutation.isPending ? 'Submitting…' : 'Submit Request'}
                            </button>
                            <button type="button" className="btn btn-outline-secondary btn-sm" onClick={resetJoinForm}>
                              Cancel
                            </button>
                          </div>
                        </div>
                      ) : (
                        <button
                          type="button"
                          className="btn btn-warning w-100 fw-bold"
                          onClick={() => {
                            setExpandedId(chit.id);
                            setJoinReason('');
                            setAgreements({
                              agreementRead: false,
                              termsAccepted: false,
                              infoProcessingAuthorized: false,
                            });
                          }}
                        >
                          <i className="bi bi-person-plus me-1" />
                          Request to Join
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      <p className="mt-4">
        <Link to="/member/dashboard">← Back to dashboard</Link>
      </p>
    </Layout>
  );
}
