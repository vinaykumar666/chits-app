import { create } from 'zustand';
import type { User } from '../types';
import { getStoredAuth, setStoredAuth } from '../api/client';
import { authApi } from '../api/auth';

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<User>;
  logout: () => void;
  loadUser: () => Promise<User | null>;
  setUser: (user: User | null) => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: getStoredAuth()?.user ?? null,
  isAuthenticated: !!getStoredAuth(),

  login: async (email, password) => {
    const { data } = await authApi.login(email, password);
    setStoredAuth(data);
    set({ user: data.user, isAuthenticated: true });
    return data.user;
  },

  logout: () => {
    setStoredAuth(null);
    set({ user: null, isAuthenticated: false });
  },

  loadUser: async () => {
    const stored = getStoredAuth();
    if (!stored) {
      set({ user: null, isAuthenticated: false });
      return null;
    }
    try {
      const { data } = await authApi.me();
      set({ user: data, isAuthenticated: true });
      return data;
    } catch {
      setStoredAuth(null);
      set({ user: null, isAuthenticated: false });
      return null;
    }
  },

  setUser: (user) => set({ user, isAuthenticated: !!user }),
}));
