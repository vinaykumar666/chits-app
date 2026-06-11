import { useEffect, useState } from 'react';
import { api } from '../api/client';

export default function NotificationBell() {
  const [count, setCount] = useState(0);

  useEffect(() => {
    let source: EventSource | null = null;
    try {
      const auth = localStorage.getItem('ygc_auth');
      const token = auth ? JSON.parse(auth).accessToken : null;
      if (!token) return;

      source = new EventSource(`/api/notifications/subscribe?token=${token}`);
      source.addEventListener('notification', () => setCount((c) => c + 1));
    } catch {
      // SSE may fail without session cookie — JWT-only clients use history polling
    }

    const poll = () => {
      api.get<string>('/api/notifications/history').then((res) => {
        try {
          const items = JSON.parse(res.data);
          if (Array.isArray(items)) setCount(items.filter((n: { read?: boolean }) => !n.read).length);
        } catch { /* ignore */ }
      }).catch(() => {});
    };
    poll();
    const id = setInterval(poll, 30000);
    return () => {
      source?.close();
      clearInterval(id);
    };
  }, []);

  if (!count) return null;
  return <span className="badge bg-danger rounded-pill">{count}</span>;
}
