import { useQuery } from '@tanstack/react-query';
import Layout from '../../components/Layout';
import NotificationBell from '../../components/NotificationBell';
import { memberApi } from '../../api/member';
import { useAuthStore } from '../../store/authStore';
import { formatCurrency, statusBadge } from '../../utils/format';
import { Link } from 'react-router-dom';

export default function MemberDashboard() {
  const user = useAuthStore((s) => s.user);
  const { data, isLoading } = useQuery({ queryKey: ['member-dashboard'], queryFn: () => memberApi.dashboard().then((r) => r.data) });

  if (isLoading) return <Layout role="MEMBER"><div className="p-4">Loading…</div></Layout>;

  return (
    <Layout role="MEMBER">
      <div className="topbar">
        <div>
          <h4><i className="bi bi-house text-warning me-2" />My Dashboard</h4>
          <div className="sub">Welcome back, {user?.fullName}</div>
        </div>
        <div className="d-flex align-items-center gap-2">
          <NotificationBell />
          <span className="user-badge">{user?.email}</span>
        </div>
      </div>

      <div className="row g-3 mb-4">
        <div className="col-md-4 col-6">
          <div className="card stat-card"><div className="card-body">
            <div className="fw-bold fs-4">{data?.memberships.length ?? 0}</div>
            <small className="text-muted">My Memberships</small>
          </div></div>
        </div>
        <div className="col-md-4 col-6">
          <div className="card stat-card"><div className="card-body">
            <div className="fw-bold fs-4">{data?.activeCount ?? 0}</div>
            <small className="text-muted">Active</small>
          </div></div>
        </div>
        <div className="col-md-4 col-6">
          <div className="card stat-card"><div className="card-body">
            <div className="fw-bold fs-4">{data?.openAuctions.length ?? 0}</div>
            <small className="text-muted">Open Auctions</small>
          </div></div>
        </div>
      </div>

      <div className="row g-4">
        <div className="col-lg-7">
          <div className="card">
            <div className="card-header d-flex justify-content-between align-items-center">
              <span>My Memberships</span>
              <Link to="/member/chits" className="btn btn-sm btn-outline-warning">Browse Chits</Link>
            </div>
            <div className="table-responsive">
              <table className="table table-hover mb-0">
                <thead><tr><th>Chit</th><th>Status</th><th>Monthly</th><th></th></tr></thead>
                <tbody>
                  {(data?.memberships ?? []).map((m) => (
                    <tr key={m.id}>
                      <td>{m.chit.name}</td>
                      <td><span className={`badge ${statusBadge(m.status)}`}>{m.status}</span></td>
                      <td>{formatCurrency(m.chit.monthlyAmount)}</td>
                      <td><Link to={`/member/memberships/${m.id}`} className="btn btn-sm btn-outline-primary">View</Link></td>
                    </tr>
                  ))}
                  {!data?.memberships.length && <tr><td colSpan={4} className="text-muted text-center py-4">No memberships yet</td></tr>}
                </tbody>
              </table>
            </div>
          </div>
        </div>
        <div className="col-lg-5">
          <div className="card">
            <div className="card-header">Open Auctions</div>
            <div className="list-group list-group-flush">
              {(data?.openAuctions ?? []).map((a) => (
                <div key={a.id} className="list-group-item">
                  <strong>{a.chitName}</strong>
                  <div className="small text-muted">Month {a.monthNumber} · {a.status}</div>
                </div>
              ))}
              {!data?.openAuctions.length && <div className="list-group-item text-muted">No open auctions</div>}
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
}
