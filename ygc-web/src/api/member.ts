import { api } from './client';
import type { Auction, Chit, ChitMembership, Payment, Settlement, User } from '../types';

export const memberApi = {
  dashboard: () =>
    api.get<{
      user: User;
      memberships: ChitMembership[];
      activeCount: number;
      availableChits: Chit[];
      openAuctions: Auction[];
      mySettlements: Settlement[];
    }>('/api/v1/member/dashboard'),

  chits: () =>
    api.get<{ chits: Chit[]; myChitStatus: Record<string, string> }>('/api/v1/member/chits'),

  chit: (id: number) => api.get<Chit>(`/api/v1/member/chits/${id}`),

  joinChit: (
    id: number,
    body: {
      agreementRead: boolean;
      termsAccepted: boolean;
      infoProcessingAuthorized: boolean;
      joinReason: string;
    },
  ) => api.post(`/api/v1/member/chits/${id}/join`, body),

  membership: (id: number) =>
    api.get<{
      membership: ChitMembership;
      payments: Payment[];
      totalPaid: number;
      auctions: Auction[];
      bidRecommendations: Record<string, unknown> | null;
      hasOpenAuction: boolean;
      mySettlements: Settlement[];
    }>(`/api/v1/member/memberships/${id}`),

  submitPayment: (membershipId: number, monthNumber: number, screenshot?: File) => {
    const form = new FormData();
    form.append('membershipId', String(membershipId));
    form.append('monthNumber', String(monthNumber));
    if (screenshot) form.append('screenshot', screenshot);
    return api.post('/api/v1/member/payments/submit', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  placeBid: (auctionId: number, bidAmount: number) =>
    api.post(`/api/v1/member/auctions/${auctionId}/bid`, { bidAmount }),

  requestExit: (membershipId: number) =>
    api.post(`/api/v1/member/memberships/${membershipId}/exit`),

  acknowledgeSettlement: (settlementId: number) =>
    api.post(`/api/v1/member/settlements/${settlementId}/acknowledge`),

  bidCalculator: (chitId: number, bidAmount?: number, monthNumber = 1) =>
    api.get<Record<string, unknown>>(`/api/v1/member/chits/${chitId}/bid-calculator`, {
      params: { bidAmount, monthNumber },
    }),
};
