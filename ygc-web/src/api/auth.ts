import { api } from './client';
import type { AuthResponse, User } from '../types';

export const authApi = {
  login: (email: string, password: string) =>
    api.post<AuthResponse>('/api/v1/auth/login', { email, password }),

  register: (data: { email: string; fullName: string; phone?: string; address?: string }) =>
    api.post<{ message: string; user: User }>('/api/v1/auth/register', data),

  me: () => api.get<User>('/api/v1/auth/me'),

  changePassword: (newPassword: string, confirmPassword: string) =>
    api.post<{ message: string }>('/api/v1/auth/change-password', { newPassword, confirmPassword }),
};
