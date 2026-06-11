import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import Layout from '../../components/Layout';
import AlertBanner from '../../components/AlertBanner';
import { adminApi } from '../../api/admin';
import { getErrorMessage } from '../../api/client';
import { formatCurrency, statusBadge } from '../../utils/format';

export default function AdminChitDetailPage() {
  const { id } = useParams<{ id: string }>();
  const chitId = Number(id);
  const queryClient = useQueryClient();
  const [alert, setAlert] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [memberEmail, setMemberEmail] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['admin-chit', chitId],
    queryFn: () => adminApi.chit(chitId).then((r) => r.data),
    enabled: !!chitId,
  });

  const approveMutation = useMutation({
    mutationFn: (membershipId: number) => adminApi.approveMembership(membershipId),
    onSuccess: () => { setAlert({ type: 'success', message: 'Membership approved!' }); queryClient.invalidateQueries({ queryKey: ['admin-chit', chitId] }); },
    onError: (err) => setAlert({ type: 'error', message: getErrorMessage(err) }),
  });

  const rejectMutation = useMutation({
    mutationFn: (membershipId: number) => adminApi.rejectMembership(membershipId),
    onSuccess: () => { setAlert({ type: 'success', message: 'Membership rejected.' }); queryClient.invalidateQueries({ queryKey: ['admin-chit', chitId] }); },
    onError: (err) => setAlert({ type: 'error', message: getErrorMessage(err) }),
  });

  const addMemberMutation = useMutation({
    mutationFn: () => adminApi.addMemberToChit(chitId, memberEmail),
    onSuccess: () => { setAlert({ type: 'success', message: 'Member added.' }); setMemberEmail(''); queryClient.invalidateQueries({ queryKey: ['admin-chit', chitId] }); },
    onError: (err) => setAlert({ type: 'error', message: getErrorMessage(err) }),
  });

  if (isLoading || !data) return <Layout role="ADMIN"><div className="p-4">Loading…</div></Layout>;

  return (
    <Layout role="ADMIN">
      <div className="topbar">
        <h4>{data.chit.name}</h4>
        <div className="sub">{data.chit.description} · <span className={`badge ${statusBadge(data.chit.status)}`}>{data.chit.status}</span></div>
      </div>
      {alert && <AlertBanner type={alert.type} message={alert.message} onClose={() => setAlert(null)} />}

      <div className="row g-3 mb-4">
        <div className="col-md-3"><div className="card stat-card"><div className="card-body"><small className="text-muted">Monthly</small><div className="fw-bold">{formatCurrency(data.chit.monthlyAmount)}</div></div></div></div>
        <div className="col-md-3"><div className="card stat-card"><div className="card-body"><small className="text-muted">Members</small><div className="fw-bold">{data.memberships.length} / {data.chit.totalMembers}</div></div></div></div>
      </div>

      <div className="card mb-4">
        <div className="card-header">Add Member by Email</div>
        <div className="card-body d-flex gap-2">
          <input className="form-control" placeholder="member@email.com" value={memberEmail} onChange={(e) => setMemberEmail(e.target.value)} />
          <button className="btn btn-warning" disabled={addMemberMutation.isPending} onClick={() => addMemberMutation.mutate()}>Add</button>
        </div>
      </div>

      <div className="card mb-4">
        <div className="card-header">Memberships</div>
        <div className="table-responsive">
          <table className="table mb-0">
            <thead><tr><th>Member</th><th>Status</th><th>Joined</th><th>Actions</th></tr></thead>
            <tbody>
              {data.memberships.map((m) => (
                <tr key={m.id}>
                  <td>{m.user?.fullName}<br /><small className="text-muted">{m.user?.email}</small></td>
                  <td><span className={`badge ${statusBadge(m.status)}`}>{m.status}</span></td>
                  <td>{m.joinedAt?.slice(0, 10)}</td>
                  <td className="d-flex gap-1">
                    {m.status === 'PENDING' && <>
                      <button className="btn btn-sm btn-success" onClick={() => approveMutation.mutate(m.id)}>Approve</button>
                      <button className="btn btn-sm btn-danger" onClick={() => rejectMutation.mutate(m.id)}>Reject</button>
                    </>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <Link to="/admin/chits">← Back to chits</Link>
    </Layout>
  );
}
