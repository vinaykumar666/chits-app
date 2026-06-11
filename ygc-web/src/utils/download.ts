import { getStoredAuth } from '../api/client';

/** Download a protected MVC report URL with JWT auth. */
export async function downloadProtectedFile(url: string, filename: string) {
  const auth = getStoredAuth();
  const res = await fetch(url, {
    headers: auth?.accessToken ? { Authorization: `Bearer ${auth.accessToken}` } : {},
  });
  if (!res.ok) throw new Error(`Download failed (${res.status})`);
  const blob = await res.blob();
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = filename;
  link.click();
  URL.revokeObjectURL(link.href);
}

export function paymentScreenshotUrl(paymentId: number) {
  return `/api/v1/admin/payments/${paymentId}/screenshot`;
}
