export function formatCurrency(value?: number | string | null) {
  const n = Number(value ?? 0);
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(n);
}

export function formatDate(value?: string | null) {
  if (!value) return '—';
  return new Date(value).toLocaleDateString('en-IN');
}

export function statusBadge(status: string) {
  const map: Record<string, string> = {
    OPEN: 'bg-primary', ACTIVE: 'bg-success', PENDING: 'bg-warning text-dark',
    APPROVED: 'bg-success', REJECTED: 'bg-danger', OVERDUE: 'bg-danger',
    COMPLETED: 'bg-secondary', CANCELLED: 'bg-dark', EXITED: 'bg-secondary',
    SETTLED: 'bg-info', ANNOUNCED: 'bg-info', CLOSED: 'bg-warning text-dark',
  };
  return map[status] || 'bg-secondary';
}
