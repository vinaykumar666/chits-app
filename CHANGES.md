# YGC Internal — Complete Feature Changelog v4.0

**Platform**: Spring Boot 3.4.1 / Java 17 / PWA  
**Tests**: 110+ unit & integration tests

---

## A. Bug Fixes (All Resolved)

| Bug | Root Cause | Fix |
|-----|-----------|-----|
| Chit delete JDBC error | CommissionLedger FK not cascaded | Added `cascade=ALL, orphanRemoval=true` |
| Emails not triggering | Hardcoded `noreply@ygcinternal.com` | Uses `@Value("${ygc.mail.from}")` |
| Auction 500 | `bids` lazy-loaded after session close | `JOIN FETCH` queries in AuctionRepository |
| Chit-history 500 | `@Lob` on String + SpEL `.?[]` in template | Removed `@Lob`, pre-computed counts in controller |
| Delete user 500 | Hard-delete violated 6 FK constraints | Soft-delete: anonymize + deactivate |
| Mobile sidebar broken | JS bailed without binding click handler | Auto-create/bind toggle for all pages |
| Toasts replay on login | SSE replayed full history on subscribe | Only send keepalive on connect, REST for history |
| `@MockBean` compile error | Deprecated in Spring Boot 3.4 | Migrated to `@MockitoBean` |
| Bell icon missing (member) | Not in member dashboard topbar | Added bell button to member templates |

---

## B. New Modules

### 1. Early Exit Module
**Model**: `EarlyExitRequest` — full lifecycle tracking

| Field | Description |
|-------|-------------|
| status | REQUESTED → UNDER_REVIEW → APPROVED / REJECTED → SETTLED |
| totalPaid | Sum of all approved payments |
| penaltyAmount | totalPaid × 2% (configurable) |
| dividendsEarned | Accumulated non-winner dividends |
| refundAmount | totalPaid - penalty + dividends |
| replacementMember | Optional replacement for the slot |
| adminRemarks | Review notes |

**Endpoints**: `GET /admin/early-exits`, `POST /admin/early-exits/{id}/process`  
**Template**: Full dashboard with approve/reject buttons

### 2. Risk & Fraud Prevention

#### Risk Score Engine (`RiskScoreService`)
Scores 0-100 based on:
- Payment behavior (50%): overdue ratio, rejection ratio, late ratio
- Login security (20%): failed login count in 30 days
- Account flags (15%): locked status, consecutive failures
- Membership stability (15%): exit/total ratio

**Risk Tiers**: LOW (0-20) → MEDIUM (21-50) → HIGH (51-75) → CRITICAL (76-100)

**Scheduled**: Recalculates nightly at 2 AM via `@Scheduled`

#### Fraud Detection Fields (User Model)
| Field | Purpose |
|-------|---------|
| aadhaarNumber | Masked Aadhaar (XXXX-XXXX-1234) |
| aadhaarVerified | Admin verification flag |
| deviceFingerprints | JSON array of known device hashes |
| lastLoginIp | Last successful login IP |
| lastLoginDevice | Extracted device type |
| consecutiveFailedLogins | Auto-locks at 5 |
| accountLocked | Prevents login |
| riskScore | 0-100 AI-computed score |

#### Login History Tracking (`LoginHistory` model)
- Records every login attempt (success/failure)
- Captures: IP, User-Agent, device fingerprint, geo-location
- Detects shared IPs (same IP, different users = fraud risk)
- Auto-locks accounts after 5 consecutive failures

**Auth Listener**: `AuthEventListener` captures Spring Security events automatically.

#### Risk Dashboard (`/admin/risk-dashboard`)
- High-risk member table with progress bars
- Recent login activity with IP/device/status
- Shared IP detection alerts

### 3. Intelligent Bid System (Game Theory)

**Sigmoid urgency model**: Low urgency early → steep mid → plateau late  
**Competition density**: Fewer eligible bidders = less pressure  
**Historical trend**: Linear regression on past winning bids  
**Dividend forecast**: What non-winners earn per month  
**Win probability**: Estimated at aggressive vs conservative bid  
**Strategy advisor**: PATIENT / BALANCED / AGGRESSIVE per phase  

### 4. Admin Notification System
4 new types: JOIN_REQUEST, PAYMENT_SUBMITTED, SETTLEMENT_REQUEST, USER_DELETED  
All push via SSE with toast notifications.

### 5. IP Address Capture
AuditService resolves real IP from X-Forwarded-For → X-Real-IP → remoteAddr.  
Displayed in audit log with search/filter/pagination.

### 6. Agreement Overhaul
- Modern PDF: navy/gold branding, dual request/approval stamps, digital seal
- Modern email: responsive HTML template with branded header/footer
- Admin can download any agreement anytime via `/admin/agreements/{id}/download`

### 7. Multilingual Support (Infrastructure)
User model includes `preferredLanguage` field (en, hi, te, ta, kn, ml).  
PDF generation can be extended with iText font packs for Devanagari, Telugu, Tamil, Kannada, Malayalam scripts.

---

## C. Audit Log Enhancements
- **Search**: Text search across all columns
- **Filter by Action**: CREATE, APPROVE, REJECT, DELETE, LOGIN, PAYMENT, AUCTION, BID
- **Filter by Date Range**: From/To date pickers
- **Pagination**: 25/50/100/All per page with prev/next
- **IP Column**: Monospace display of client IP

---

## D. User Management
- **Inline create**: Collapsible form on admin members page
- **Soft-delete**: Anonymize + deactivate (preserves FK integrity)
- **Email notification**: Deleted users receive email with reason
- **Password reset**: Sends temp password via email

---

## E. UI/UX
- Glassmorphism CSS with gold gradient system
- Inter font, micro-animations, spring easing
- Mobile-first: safe-area-insets, bottom nav, swipe-to-close
- iOS fixes: 100vh, momentum scroll, tap highlight
- PWA: manifest with shortcuts, display_override
- Company logo in all sidebars with fallback icon

---

## F. File Inventory

### New Java Files (8)
- `LoginHistory.java` — Login tracking model
- `EarlyExitRequest.java` — Early exit lifecycle model
- `LoginHistoryRepository.java` — Login queries
- `EarlyExitRequestRepository.java` — Exit queries
- `RiskScoreService.java` — AI risk scoring + defaulter prediction
- `EarlyExitService.java` — Exit workflow: request → review → settle
- `LoginTrackingService.java` — Login recording + fraud detection
- `AuthEventListener.java` — Spring Security event capture

### Modified Java Files (12)
- `User.java` — Aadhaar, device, risk, language fields
- `ChitHistory.java` — Removed `@Lob`, uses CLOB
- `Notification.java` — 4 admin notification types
- `AuditService.java` — IP capture
- `NotificationService.java` — Admin notifiers, no SSE replay
- `ChitAgreementService.java` — PDF stamps, branded email, admin email fix
- `BidCalculationService.java` — Game-theory engine
- `AuctionRepository.java` — JOIN FETCH queries
- `AuctionService.java` — Uses JOIN FETCH
- `AdminController.java` — Early exits, risk, agreements, delete user
- `PaymentRepository.java` — findByMembershipUser
- `pom.xml` — Spring Boot 3.4.1

### Templates (5 new, 8 modified)
- NEW: `early-exits.html`, `risk-dashboard.html`
- Modified: `fragments.html`, `audit.html`, `chit-history.html`, `auctions.html`, `members.html`, `dashboard.html` (member), `login.html`, `register.html`

---

*YGC Internal v4.0 — Save Rupee, Rupee Will Save You In Future*
