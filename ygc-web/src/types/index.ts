export type Role = 'ADMIN' | 'MEMBER';

export interface User {
  id: number;
  email: string;
  fullName: string;
  phone?: string;
  address?: string;
  role: Role;
  firstLogin: boolean;
  active: boolean;
  termsAccepted: boolean;
  createdAt?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}

export interface Chit {
  id: number;
  name: string;
  description?: string;
  monthlyAmount: number;
  totalMembers: number;
  durationMonths: number;
  totalChitValue?: number;
  adminCommissionPercentage: number;
  minBidAmount?: number;
  maxBidAmount?: number;
  startDate: string;
  endDate?: string;
  status: 'OPEN' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
  closingReason?: string;
  memberCount?: number;
}

export interface ChitMembership {
  id: number;
  chit: Chit;
  user?: User;
  status: 'PENDING' | 'ACTIVE' | 'SETTLED' | 'EXITED';
  hasWonAuction: boolean;
  termsAccepted: boolean;
  rejectionReason?: string;
  agreementRead: boolean;
  agreementAccepted: boolean;
  infoProcessingAuthorized: boolean;
  agreementNumber?: string;
  joinReason?: string;
  joinedAt?: string;
}

export interface Payment {
  id: number;
  membershipId?: number;
  memberName?: string;
  chitName?: string;
  amount: number;
  lateFine?: number;
  totalAmount?: number;
  adminRemarks?: string;
  rejectionReason?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'OVERDUE';
  dueDate?: string;
  paidDate?: string;
  monthNumber?: number;
  createdAt?: string;
}

export interface Auction {
  id: number;
  chitId?: number;
  chitName?: string;
  monthNumber?: number;
  auctionDate?: string;
  status: 'ANNOUNCED' | 'OPEN' | 'CLOSED' | 'COMPLETED';
  winner?: User;
  winningBidAmount?: number;
  lumpSumPayout?: number;
  adminCommission?: number;
  payoutReleased: boolean;
}

export interface Settlement {
  id: number;
  membershipId?: number;
  memberName?: string;
  chitName?: string;
  totalPaidAmount?: number;
  pendingDues?: number;
  lateFines?: number;
  finalSettlementAmount?: number;
  type?: 'EARLY_EXIT' | 'MATURITY';
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'COMPLETED';
  adminRemarks?: string;
  requestedAt?: string;
}

export interface AuditLog {
  id: number;
  userEmail?: string;
  userName?: string;
  action: string;
  entityType?: string;
  entityId?: number;
  description?: string;
  timestamp: string;
}

export interface CommissionLedgerEntry {
  id: number;
  chitId?: number;
  chitName?: string;
  source?: string;
  month?: string;
  commissionAmount: number;
  commissionPercentage?: number;
}

export interface ChitHistory {
  id: number;
  chitName: string;
  finalStatus: string;
  closingReason?: string;
  closedAt?: string;
}

export interface ApiError {
  message?: string;
  error?: string;
}
