import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import Layout from '../../components/Layout';
import AlertBanner from '../../components/AlertBanner';
import { adminApi } from '../../api/admin';
import { getErrorMessage } from '../../api/client';

export default function AdminMembersPage() {
  const queryClient = useQueryClient();
  const [alert, setAlert] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [form, setForm] = useState({ email: '', fullName: '', phone: '', address: '' });

  const { data: members = [], isLoading } = useQuery({
    queryKey: ['admin-members'],
    queryFn: () => adminApi.members().then((r) => r.data),
  });

  const createMutation = useMutation({
    mutationFn: () => adminApi.createMember(form),
    onSuccess: () => { setAlert({ type: 'success', message: 'Member created.' }); queryClient.invalidateQueries({ queryKey: ['admin-members'] }); },
    onError: (err) => setAlert({ type: 'error', message: getErrorMessage(err) }),
  });

  const toggleMutation = useMutation({
    mutationFn: (id: number) => adminApi.toggleStatus(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-members'] }),
    onError: (err) => setAlert({ type: 'error', message: getErrorMessage(err) }),
  });

  if (isLoading) return <Layout role="ADMIN"><div className="p-4">Loading…</div></Layout>;

  return (
    <Layout role="ADMIN">
      <div className="topbar"><h4><i className="bi bi-people text-warning me-2" />Members</h4></div>
      {alert && <AlertBanner type={alert.type} message={alert.message} onClose={() => setAlert(null)} />}

      <div className="card mb-4"><div className="card-header">Create Member</div><div className="card-body row g-2">
        {(['email', 'fullName', 'phone', 'address'] as const).map((f) => (
          <div className="col-md-3" key={f}>
            <input className="form-control" placeholder={f} value={form[f]} onChange={(e) => setForm({ ...form, [f]: e.target.value })} />
          </div>
        ))}
        <div className="col-md-3"><button className="btn btn-warning w-100" onClick={() => createMutation.mutate()}>Create</button></div>
      </div></div>

      <div className="table-responsive card">
        <table className="table mb-0">
          <thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>
            {members.filter((m) => m.role === 'MEMBER').map((m) => (
              <tr key={m.id}>
                <td>{m.fullName}</td>
                <td>{m.email}</td>
                <td>{m.role}</td>
                <td><span className={`badge ${m.active ? 'bg-success' : 'bg-secondary'}`}>{m.active ? 'Active' : 'Inactive'}</span></td>
                <td><button className="btn btn-sm btn-outline-warning" onClick={() => toggleMutation.mutate(m.id)}>Toggle Status</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Layout>
  );
}
