import { api } from './client';
import type {
  AuditLog,
  Auction,
  Chit,
  ChitHistory,
  ChitMembership,
  CommissionLedgerEntry,
  DocumentRecord,
  DuplicateRecord,
  EarlyExitRequest,
  LoginHistoryEntry,
  LoginTrackingData,
  MemberProfileData,
  Payment,
  RiskAlert,
  Settlement,
  User,
} from '../types';

export const adminApi = {
  dashboard: () =>
    api.get<{
      user: User;
      totalChits: number;
      totalMembers: number;
      pendingPayments: number;
      pendingSettlements: number;
      openAuctions: number;
      recentAudits: AuditLog[];
    }>('/api/v1/admin/dashboard'),

  chits: () => api.get<Chit[]>('/api/v1/admin/chits'),

  createChit: (body: Record<string, unknown>) => api.post<Chit>('/api/v1/admin/chits', body),

  chit: (id: number) =>
    api.get<{ chit: Chit; memberships: ChitMembership[]; auctions: Auction[] }>(`/api/v1/admin/chits/${id}`),

  updateChit: (id: number, body: Record<string, unknown>) => api.put<Chit>(`/api/v1/admin/chits/${id}`, body),

  deleteChit: (id: number) => api.delete(`/api/v1/admin/chits/${id}`),

  approveMembership: (id: number) => api.post(`/api/v1/admin/memberships/${id}/approve`),

  rejectMembership: (id: number, reason?: string) =>
    api.post(`/api/v1/admin/memberships/${id}/reject`, { reason }),

  payments: () =>
    api.get<{ pendingPayments: Payment[]; allPayments: Payment[] }>('/api/v1/admin/payments'),

  verifyPayment: (id: number, approved: boolean, remarks?: string) =>
    api.post(`/api/v1/admin/payments/${id}/verify`, { approved, remarks }),

  auctions: () =>
    api.get<{
      chits: Chit[];
      openAuctions: Auction[];
      allAuctions: Auction[];
      bidRecommendations: Record<number, Record<string, unknown>>;
    }>('/api/v1/admin/auctions'),

  createAuction: (body: { chitId: number; monthNumber: number; auctionDate: string }) =>
    api.post('/api/v1/admin/auctions', body),

  openAuction: (id: number) => api.post(`/api/v1/admin/auctions/${id}/open`),

  closeAuction: (id: number) => api.post(`/api/v1/admin/auctions/${id}/close`),

  releasePayout: (id: number) => api.post(`/api/v1/admin/auctions/${id}/release-payout`),

  settlements: () =>
    api.get<{ pendingSettlements: Settlement[]; allSettlements: Settlement[] }>('/api/v1/admin/settlements'),

  processSettlement: (id: number, approved: boolean, remarks?: string) =>
    api.post(`/api/v1/admin/settlements/${id}/process`, { approved, remarks }),

  commissionReport: () =>
    api.get<{ ledger: CommissionLedgerEntry[]; chits: Chit[]; totalCommission: number }>(
      '/api/v1/admin/reports/commission',
    ),

  members: () => api.get<User[]>('/api/v1/admin/members'),

  createMember: (body: { email: string; fullName: string; phone?: string; address?: string }) =>
    api.post('/api/v1/admin/members', body),

  updateMember: (id: number, body: Record<string, unknown>) => api.put<User>(`/api/v1/admin/members/${id}`, body),

  resetPassword: (id: number) => api.post(`/api/v1/admin/members/${id}/reset-password`),

  toggleStatus: (id: number) => api.post(`/api/v1/admin/members/${id}/toggle-status`),

  deleteMember: (id: number, reason?: string) =>
    api.delete(`/api/v1/admin/members/${id}`, { params: { reason } }),

  sendAnnouncement: (title: string, message: string, target = 'all') =>
    api.post('/api/v1/admin/announcements', { title, message, target }),

  audit: () => api.get<AuditLog[]>('/api/v1/admin/audit'),

  chitHistory: () =>
    api.get<{
      histories: ChitHistory[];
      deletedCount: number;
      completedCount: number;
      cancelledCount: number;
    }>('/api/v1/admin/chit-history'),

  earlyExits: () =>
    api.get<{ requests: EarlyExitRequest[] }>('/api/v1/admin/early-exits'),

  processEarlyExit: (id: number, approved: boolean, remarks?: string) =>
    api.post(`/api/v1/admin/early-exits/${id}/process`, { approved, remarks }),

  riskDashboard: () =>
    api.get<{ alerts: RiskAlert[]; recentLogins: LoginHistoryEntry[] }>('/api/v1/admin/risk-dashboard'),

  fraudDetection: () =>
    api.get<{
      duplicateAadhaar: DuplicateRecord[];
      duplicatePhone: DuplicateRecord[];
      highRiskMembers: RiskAlert[];
      watchlistMembers: User[];
    }>('/api/v1/admin/fraud-detection'),

  loginTracking: () => api.get<LoginTrackingData>('/api/v1/admin/login-tracking'),

  toggleAadhaar: (id: number) => api.post(`/api/v1/admin/members/${id}/toggle-aadhaar`),

  resetLoginCounter: (id: number) => api.post(`/api/v1/admin/members/${id}/reset-login-counter`),

  memberProfile: (id: number) => api.get<MemberProfileData>(`/api/v1/admin/members/${id}/profile`),

  documents: () =>
    api.get<{
      agreements: DocumentRecord[];
      certificates: DocumentRecord[];
      settlements: Settlement[];
    }>('/api/v1/admin/documents'),

  paymentScreenshotUrl: (id: number) => `/api/v1/admin/payments/${id}/screenshot`,

  addMemberToChit: (chitId: number, memberEmail: string) =>
    api.post(`/api/v1/admin/chits/${chitId}/members`, { memberEmail }),

  removeMemberFromChit: (chitId: number, membershipId: number, reason: string) =>
    api.post(`/api/v1/admin/chits/${chitId}/members/${membershipId}/remove`, { reason }),
};
