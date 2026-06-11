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
  userAcknowledged?: boolean;
  acknowledgedAt?: string;
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

export interface EarlyExitRequest {
  id: number;
  membershipId?: number;
  memberName?: string;
  memberEmail?: string;
  chitName?: string;
  status: string;
  reason?: string;
  totalPaid?: number;
  penaltyAmount?: number;
  refundAmount?: number;
  adminRemarks?: string;
  requestedAt?: string;
}

export interface LoginHistoryEntry {
  id: number;
  userEmail?: string;
  userName?: string;
  ipAddress?: string;
  userAgent?: string;
  success: boolean;
  failureReason?: string;
  loginAt?: string;
}

export interface RiskAlert {
  user: User;
  riskScore: number;
  tier: string;
  color?: string;
}

export interface DuplicateRecord {
  value?: string;
  emails?: string;
  count?: string;
  [key: string]: string | undefined;
}

export interface SecurityUser extends User {
  aadhaarVerified?: boolean;
  accountLocked?: boolean;
  consecutiveFailedLogins?: number;
  lastLoginIp?: string;
  lastLoginAt?: string;
  riskScore?: number;
  trustRating?: string;
}

export interface LoginTrackingData {
  recentLogins: LoginHistoryEntry[];
  lockedAccounts: User[];
  failedLoginUsers: User[];
  allMembers: SecurityUser[];
  aadhaarVerified: number;
  aadhaarPending: number;
  totalLogins: number;
  failedLogins: number;
  successRate: number;
  uniqueIPs: number;
  aadhaarCompliancePct: number;
  suspiciousUsers: string[];
  suspiciousCount: number;
}

export interface MemberProfileData {
  member: SecurityUser;
  memberships: ChitMembership[];
  activeChits: number;
  completedChits: number;
  exitedChits: number;
  totalPayments: number;
  onTimePayments: number;
  overduePayments: number;
  paymentScore: number;
  riskScore: number;
  trustRating: string;
  loginHistory: LoginHistoryEntry[];
}

export interface DocumentRecord {
  membershipId: number;
  memberName: string;
  chitName: string;
  path: string;
  agreementNumber?: string;
}

export interface ApiError {
  message?: string;
  error?: string;
}
