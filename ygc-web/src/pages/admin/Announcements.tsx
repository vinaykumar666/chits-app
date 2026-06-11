import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import Layout from '../../components/Layout';
import AlertBanner from '../../components/AlertBanner';
import { adminApi } from '../../api/admin';
import { getErrorMessage } from '../../api/client';

export default function AnnouncementsPage() {
  const [alert, setAlert] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [form, setForm] = useState({ title: '', message: '', target: 'all' });

  const mutation = useMutation({
    mutationFn: () => adminApi.sendAnnouncement(form.title, form.message, form.target),
    onSuccess: (res) => { setAlert({ type: 'success', message: res.data.message }); setForm({ title: '', message: '', target: 'all' }); },
    onError: (err) => setAlert({ type: 'error', message: getErrorMessage(err) }),
  });

  return (
    <Layout role="ADMIN">
      <div className="topbar"><h4><i className="bi bi-megaphone text-warning me-2" />Announcements</h4></div>
      {alert && <AlertBanner type={alert.type} message={alert.message} onClose={() => setAlert(null)} />}
      <div className="card"><div className="card-body">
        <div className="mb-3"><label className="form-label">Title</label><input className="form-control" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></div>
        <div className="mb-3"><label className="form-label">Message</label><textarea className="form-control" rows={4} value={form.message} onChange={(e) => setForm({ ...form, message: e.target.value })} /></div>
        <button className="btn btn-warning" disabled={mutation.isPending || !form.title || !form.message} onClick={() => mutation.mutate()}>Send to All Members</button>
      </div></div>
    </Layout>
  );
}
