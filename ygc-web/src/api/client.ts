import axios from 'axios';
import type { AuthResponse } from '../types';

const STORAGE_KEY = 'ygc_auth';

export function getStoredAuth(): AuthResponse | null {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AuthResponse;
  } catch {
    return null;
  }
}

export function setStoredAuth(auth: AuthResponse | null) {
  if (auth) localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
  else localStorage.removeItem(STORAGE_KEY);
}

export const api = axios.create({
  baseURL: '',
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const auth = getStoredAuth();
  if (auth?.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`;
  }
  return config;
});

let refreshing: Promise<AuthResponse> | null = null;

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true;
      const auth = getStoredAuth();
      if (auth?.refreshToken) {
        try {
          refreshing ??= api
            .post<AuthResponse>('/api/v1/auth/refresh', { refreshToken: auth.refreshToken })
            .then((r) => {
              setStoredAuth(r.data);
              return r.data;
            })
            .finally(() => {
              refreshing = null;
            });
          const newAuth = await refreshing;
          original.headers.Authorization = `Bearer ${newAuth.accessToken}`;
          return api(original);
        } catch {
          setStoredAuth(null);
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  },
);

export function getErrorMessage(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as { message?: string; error?: string } | undefined;
    return data?.message || data?.error || err.message;
  }
  if (err instanceof Error) return err.message;
  return 'Something went wrong';
}
