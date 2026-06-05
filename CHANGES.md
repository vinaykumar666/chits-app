# YGC Internal Chit Management — Comprehensive Changes Document

**Version**: 3.0.0  
**Date**: June 2026  
**Platform**: Spring Boot 3.4.1 / Java 17 / H2 (dev) / PWA

---

## 1. Critical Bug Fixes

### 1.1 JDBC Error on Chit Delete (FK Constraint Violation)
**Root cause**: `CommissionLedger` had a `@ManyToOne` FK to `Chit` but `Chit` didn't cascade deletes to it.  
**Fix**: Added `@OneToMany(cascade = ALL, orphanRemoval = true)` for `commissionLedgerEntries` in `Chit.java`. Changed `deleteById()` → `delete(entity) + flush()` with `@Transactional` in both delete endpoints.

### 1.2 Emails Not Triggering on Admin Adding User
**Root cause**: `EmailService` hardcoded `setFrom("noreply@ygcinternal.com")` — Gmail SMTP rejected it.  
**Fix**: Injected `@Value("${ygc.mail.from}")` and used the configured sender address.

### 1.3 Auction Page 500 Error (LazyInitializationException)
**Root cause**: `Auction.bids` is `@OneToMany` (lazy by default). With `open-in-view=false`, the session closed before Thymeleaf rendered `auction.bids.size()`.  
**Fix**: Added `findAllWithBidsAndChit()` and `findByStatusWithBidsAndChit()` with `JOIN FETCH` to `AuctionRepository`. Updated `AuctionService` to use them.

### 1.4 Chit History 500 Error (SpEL + Thymeleaf Incompatibility)
**Root cause**: Template used SpEL collection selection `.?[finalStatus == 'DELETED']` inside Thymeleaf's `#lists.size()` — the two expression engines don't interop.  
**Fix**: Pre-computed `deletedCount`, `completedCount`, `cancelledCount` in the controller. Template uses simple `${deletedCount}`.

### 1.5 Mobile Sidebar Not Working (iPhone / All Devices)
**Root causes**:  
1. Templates had `<button id="ygc-mob-toggle">` in HTML → JS saw it → `return;` without binding click handler.  
2. `initTouchImprovements()` called `e.preventDefault()` on all touchend events → broke iOS Safari.  
3. CSS overlay had `display: block` → visible on page load.  
**Fix**: JS auto-creates button if absent OR binds handler to existing one. Removed aggressive `preventDefault`. Overlay defaults to `display: none`. Added swipe-left-to-close gesture and ESC key handler.

---

## 2. Agreement System Overhaul

### 2.1 Agreement PDF Redesign
- YGC brand logo placeholder with navy/gold color scheme
- Dual-stamp layout: **MEMBER REQUEST** (blue) + **★ APPROVED ★** (green)
- Digital seal with agreement reference number
- Professional typography with section headers (§)
- Customer address field included
- Non-winner dividend terms in clause 5

### 2.2 Agreement Email Modernization
- Responsive HTML email template with dark header + gold branding
- Structured agreement details card with all chit parameters
- Gold-accent notice box for PDF attachment callout
- Branded footer with contact info

### 2.3 Admin Agreement Download
- New endpoint: `GET /admin/agreements/{membershipId}/download`
- Auto-regenerates PDF if file missing from disk
- Download button in chit-detail membership table
- Admin can download any agreement at any time

### 2.4 Fixed Admin Email Address
- Replaced hardcoded `admin@ygcinternal.com` with `@Value("${ygc.mail.from}")` injection

---

## 3. Admin Notification System

### 3.1 New Admin Notification Types
| Type | Trigger | Message |
|------|---------|---------|
| `ADMIN_JOIN_REQUEST` | Member requests to join a chit | "Member X requested to join 'Chit Y'" |
| `ADMIN_PAYMENT_SUBMITTED` | Member submits a payment | "Member X submitted ₹Z for 'Chit Y'" |
| `ADMIN_SETTLEMENT_REQUEST` | Member requests settlement | "Member X requested settlement for 'Chit Y'" |
| `ADMIN_USER_DELETED` | Admin deletes a user | "User 'X' permanently removed" |

### 3.2 SSE Integration
All admin notification types are registered as SSE event listeners in `app.js` with appropriate icons and toast styles.

---

## 4. User Management

### 4.1 Admin Delete User (Complete Removal)
**Endpoint**: `POST /admin/members/{id}/delete`  
**Flow**:
1. Validates user is not an ADMIN
2. Exits all active/pending memberships with reason
3. Audit-logs the deletion with IP address
4. Sends email notification to deleted user with reason
5. Pushes admin notification
6. Hard-deletes the user record

### 4.2 Admin Create User (Inline)
- Collapsible form on admin members page
- Creates user + sends temp password email
- No longer redirects to public `/register`

---

## 5. IP Address Capture

### 5.1 AuditService Enhancement
- Injects `HttpServletRequest` via `RequestContextHolder`
- Resolves real client IP from `X-Forwarded-For` → `X-Real-IP` → `getRemoteAddr()`
- Handles proxy chains (Nginx, ALB, CloudFront)
- Defaults to "SYSTEM" for non-request contexts (schedulers, async tasks)

### 5.2 Audit Log Display
- IP address column added to admin audit table
- Displayed as monospace code for readability

---

## 6. Intelligent Bid System (Game Theory Engine)

### 6.1 Core Mechanics
- **Lowest bidder wins** → sacrifices maximum discount for immediate liquidity
- **Non-winners earn dividends** → discount distributed proportionally
- **Patience = Profit** → staying longer accumulates more dividends

### 6.2 Calculation Model
| Factor | Description |
|--------|-------------|
| **Urgency Decay** | Sigmoid curve: low urgency early (bids near chit value) → high urgency late (bids drop) |
| **Competition Density** | `eligibleBidders / totalMembers` — more competitors → harder bidding |
| **Historical Trend** | Linear regression on past winning bids — detects rising/falling patterns |
| **Regulatory Floor** | 70% of chit value (RBI guideline) |

### 6.3 New Recommendation Fields
| Field | Description |
|-------|-------------|
| `eligibleBidders` | Members who haven't won yet |
| `competitionFactor` | 0.0–1.0 competition intensity |
| `urgencyScore` | 0–100 urgency level |
| `dividendPerMember` | What non-winners earn this month |
| `projectedTotalDividend` | Cumulative dividend if member stays till end |
| `winProbLow` | Win probability at aggressive (low) bid |
| `winProbHigh` | Win probability at conservative (high) bid |
| `bidTrend` | Historical bid direction (+ve rising, -ve falling) |
| `strategy` | PATIENT / BALANCED / AGGRESSIVE advice text |

### 6.4 Strategy Advisor
- **Months 1–30%**: PATIENT — "Non-winners earn dividends. Bid high unless urgent."
- **Months 30–70%**: BALANCED — "Competition peaks. Bid in recommended range."
- **Months 70–100%**: AGGRESSIVE — "Few bidders remain. Good win opportunity."

---

## 7. UI/UX Overhaul

### 7.1 CSS Design System
- Glassmorphism with `backdrop-filter: blur()`
- Gold gradient system (`--gradient-gold`, `--gradient-dark`)
- Inter font from Google Fonts
- Micro-animations: hover lift, spring easing, pulse-dot
- 14px card radius, subtle layered shadows

### 7.2 Mobile-First PWA
- Safe-area-insets for notch/home-bar devices
- Bottom navigation bar (5 tabs: admin/member adaptive)
- Swipe-to-close sidebar gesture
- iOS-specific fixes (100vh, momentum scroll, tap highlight)
- Enhanced manifest with shortcuts and display_override

### 7.3 Company Logo
- Brand logo (`/images/ygc-logo.png`) in both admin and member sidebars
- Graceful fallback to "Y" brand icon if image fails to load

---

## 8. Test Suite (107 Tests)

| Test Class | Count | Coverage |
|------------|-------|----------|
| UserServiceTest | 10 | Registration, passwords, find, duplicates |
| ChitServiceTest | 15 | Create, join, approve, validation, queries |
| EmailServiceTest | 14 | All email methods, failure handling |
| BidCalculationServiceTest | 8 | Game-theory recommendations, strategy, dividends |
| AuthControllerTest | 9 | Login, register, dashboard routing, password change |
| RepositoryTests | 9 | CRUD, cascade delete verification, membership queries |
| ModelTests | 18 | All entity defaults, enums, Notification JSON |
| JwtUtilTest | 6 | Token generate, validate, extract, expired, malformed |
| AuditServiceTest | 2 | IP capture, null handling |
| DataInitializerTest | 2 | Admin seed, skip-if-exists |
| ExceptionTests | 4 | All custom exception classes |

**Spring Boot**: 3.4.1 (upgraded from 3.2.0)  
**Test annotations**: `@MockitoBean` (replaces deprecated `@MockBean`)

---

## 9. Files Modified

### Java Source (6 new, 8 modified)
- `AuditService.java` — IP capture from HttpServletRequest
- `NotificationService.java` — 4 admin notification methods
- `Notification.java` — 4 new enum types
- `ChitAgreementService.java` — PDF redesign, email redesign, admin email fix
- `BidCalculationService.java` — Game-theory engine rewrite
- `AuctionRepository.java` — JOIN FETCH queries
- `AuctionService.java` — Uses JOIN FETCH queries
- `AdminController.java` — Delete user, agreement download, chit-history fix, admin create user

### Templates (5 rewritten, 3 updated)
- `fragments.html` — Logo, Inter font, PWA meta
- `login.html` / `register.html` — Glassmorphism dark theme
- `admin/dashboard.html` — Fragment-based, mobile toggle
- `admin/members.html` — Inline create form, delete button
- `admin/auctions.html` — Fragment-based, responsive
- `admin/chit-history.html` — Pre-computed counts, fragment-based
- `admin/audit.html` — IP address column
- `admin/chit-detail.html` — Agreement download button

### CSS / JS / Config
- `app.css` — Complete redesign (glassmorphism, mobile cards, iOS fixes)
- `app.js` — Mobile sidebar fix, admin SSE handlers, touch fix
- `manifest.json` — Enhanced PWA config
- `pom.xml` — Spring Boot 3.4.1

---

*YGC Internal — Save Rupee, Rupee Will Save You In Future*
