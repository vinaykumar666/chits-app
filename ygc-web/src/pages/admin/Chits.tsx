import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import Layout from '../../components/Layout';
import AlertBanner from '../../components/AlertBanner';
import { adminApi } from '../../api/admin';
import { getErrorMessage } from '../../api/client';
import { formatCurrency, statusBadge } from '../../utils/format';

export default function AdminChitsPage() {
  const queryClient = useQueryClient();
  const [alert, setAlert] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({
    name: '', description: '', monthlyAmount: '', totalMembers: '', durationMonths: '',
    adminCommissionPercentage: '5', startDate: new Date().toISOString().slice(0, 10),
  });

  const { data: chits = [], isLoading } = useQuery({
    queryKey: ['admin-chits'],
    queryFn: () => adminApi.chits().then((r) => r.data),
  });

  const createMutation = useMutation({
    mutationFn: () => adminApi.createChit({ ...form, monthlyAmount: Number(form.monthlyAmount), totalMembers: Number(form.totalMembers), durationMonths: Number(form.durationMonths), adminCommissionPercentage: Number(form.adminCommissionPercentage) }),
    onSuccess: () => {
      setAlert({ type: 'success', message: 'Chit created successfully!' });
      setShowForm(false);
      queryClient.invalidateQueries({ queryKey: ['admin-chits'] });
    },
    onError: (err) => setAlert({ type: 'error', message: getErrorMessage(err) }),
  });

  if (isLoading) return <Layout role="ADMIN"><div className="p-4">Loading…</div></Layout>;

  return (
    <Layout role="ADMIN">
      <div className="topbar d-flex justify-content-between align-items-center">
        <h4><i className="bi bi-collection text-warning me-2" />Chit Groups</h4>
        <button className="btn btn-warning" onClick={() => setShowForm(!showForm)}>{showForm ? 'Cancel' : 'Create Chit'}</button>
      </div>
      {alert && <AlertBanner type={alert.type} message={alert.message} onClose={() => setAlert(null)} />}

      {showForm && (
        <div className="card mb-4"><div className="card-body">
          <div className="row g-3">
            {Object.entries(form).map(([key, val]) => (
              <div className="col-md-6" key={key}>
                <label className="form-label text-capitalize">{key.replace(/([A-Z])/g, ' $1')}</label>
                <input className="form-control" value={val} onChange={(e) => setForm({ ...form, [key]: e.target.value })} />
              </div>
            ))}
          </div>
          <button className="btn btn-warning mt-3" disabled={createMutation.isPending} onClick={() => createMutation.mutate()}>Save Chit</button>
        </div></div>
      )}

      <div className="table-responsive card">
        <table className="table table-hover mb-0">
          <thead><tr><th>Name</th><th>Monthly</th><th>Members</th><th>Status</th><th>Start</th><th></th></tr></thead>
          <tbody>
            {chits.map((c) => (
              <tr key={c.id}>
                <td>{c.name}</td>
                <td>{formatCurrency(c.monthlyAmount)}</td>
                <td>{c.totalMembers}</td>
                <td><span className={`badge ${statusBadge(c.status)}`}>{c.status}</span></td>
                <td>{c.startDate}</td>
                <td><Link to={`/admin/chits/${c.id}`} className="btn btn-sm btn-outline-primary">Manage</Link></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Layout>
  );
}
