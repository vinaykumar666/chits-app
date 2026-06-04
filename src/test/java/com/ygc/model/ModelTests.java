package com.ygc.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class ModelTests {

    @Nested
    @DisplayName("User Model")
    class UserModelTest {

        @Test
        @DisplayName("should have correct defaults")
        void shouldHaveDefaults() {
            User user = new User();
            assertThat(user.getRole()).isEqualTo(User.Role.MEMBER);
            assertThat(user.isFirstLogin()).isTrue();
            assertThat(user.isActive()).isTrue();
            assertThat(user.isTermsAccepted()).isFalse();
            assertThat(user.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should support all roles")
        void shouldSupportAllRoles() {
            assertThat(User.Role.values()).containsExactly(User.Role.ADMIN, User.Role.MEMBER);
        }

        @Test
        @DisplayName("should set and get all fields")
        void shouldSetAndGetFields() {
            User user = new User();
            user.setId(1L);
            user.setEmail("test@test.com");
            user.setPassword("pass");
            user.setFullName("Test");
            user.setPhone("123");
            user.setAddress("Addr");
            user.setRole(User.Role.ADMIN);
            user.setFirstLogin(false);
            user.setActive(false);
            user.setTermsAccepted(true);

            assertThat(user.getId()).isEqualTo(1L);
            assertThat(user.getEmail()).isEqualTo("test@test.com");
            assertThat(user.getFullName()).isEqualTo("Test");
            assertThat(user.getRole()).isEqualTo(User.Role.ADMIN);
        }
    }

    @Nested
    @DisplayName("Chit Model")
    class ChitModelTest {

        @Test
        @DisplayName("should have correct defaults")
        void shouldHaveDefaults() {
            Chit chit = new Chit();
            assertThat(chit.getStatus()).isEqualTo(Chit.ChitStatus.OPEN);
            assertThat(chit.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should support all statuses")
        void shouldSupportStatuses() {
            assertThat(Chit.ChitStatus.values()).containsExactly(
                    Chit.ChitStatus.OPEN, Chit.ChitStatus.ACTIVE,
                    Chit.ChitStatus.COMPLETED, Chit.ChitStatus.CANCELLED);
        }

        @Test
        @DisplayName("should compute total value from monthly * members")
        void shouldSetFields() {
            Chit chit = new Chit();
            chit.setMonthlyAmount(new BigDecimal("5000"));
            chit.setTotalMembers(10);
            chit.setTotalChitValue(chit.getMonthlyAmount().multiply(BigDecimal.valueOf(chit.getTotalMembers())));

            assertThat(chit.getTotalChitValue()).isEqualByComparingTo("50000");
        }
    }

    @Nested
    @DisplayName("ChitMembership Model")
    class ChitMembershipModelTest {

        @Test
        @DisplayName("should have correct defaults")
        void shouldHaveDefaults() {
            ChitMembership m = new ChitMembership();
            assertThat(m.getStatus()).isEqualTo(ChitMembership.MembershipStatus.PENDING);
            assertThat(m.isHasWonAuction()).isFalse();
            assertThat(m.isTermsAccepted()).isFalse();
            assertThat(m.isAgreementRead()).isFalse();
            assertThat(m.isAgreementAccepted()).isFalse();
            assertThat(m.isInfoProcessingAuthorized()).isFalse();
        }

        @Test
        @DisplayName("should support all membership statuses")
        void shouldSupportStatuses() {
            assertThat(ChitMembership.MembershipStatus.values()).containsExactly(
                    ChitMembership.MembershipStatus.PENDING,
                    ChitMembership.MembershipStatus.ACTIVE,
                    ChitMembership.MembershipStatus.SETTLED,
                    ChitMembership.MembershipStatus.EXITED);
        }
    }

    @Nested
    @DisplayName("Payment Model")
    class PaymentModelTest {

        @Test
        @DisplayName("should have correct defaults")
        void shouldHaveDefaults() {
            Payment p = new Payment();
            assertThat(p.getStatus()).isEqualTo(Payment.PaymentStatus.PENDING);
            assertThat(p.getLateFine()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should support all payment statuses")
        void shouldSupportStatuses() {
            assertThat(Payment.PaymentStatus.values()).containsExactly(
                    Payment.PaymentStatus.PENDING, Payment.PaymentStatus.APPROVED,
                    Payment.PaymentStatus.REJECTED, Payment.PaymentStatus.OVERDUE);
        }
    }

    @Nested
    @DisplayName("Auction Model")
    class AuctionModelTest {

        @Test
        @DisplayName("should have correct defaults")
        void shouldHaveDefaults() {
            Auction a = new Auction();
            assertThat(a.getStatus()).isEqualTo(Auction.AuctionStatus.ANNOUNCED);
            assertThat(a.isPayoutReleased()).isFalse();
        }

        @Test
        @DisplayName("should support all auction statuses")
        void shouldSupportStatuses() {
            assertThat(Auction.AuctionStatus.values()).containsExactly(
                    Auction.AuctionStatus.ANNOUNCED, Auction.AuctionStatus.OPEN,
                    Auction.AuctionStatus.CLOSED, Auction.AuctionStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("Settlement Model")
    class SettlementModelTest {

        @Test
        @DisplayName("should have correct defaults")
        void shouldHaveDefaults() {
            Settlement s = new Settlement();
            assertThat(s.getStatus()).isEqualTo(Settlement.SettlementStatus.PENDING);
        }

        @Test
        @DisplayName("should support all types and statuses")
        void shouldSupportEnums() {
            assertThat(Settlement.SettlementType.values())
                    .containsExactly(Settlement.SettlementType.EARLY_EXIT, Settlement.SettlementType.MATURITY);
            assertThat(Settlement.SettlementStatus.values())
                    .containsExactly(Settlement.SettlementStatus.PENDING, Settlement.SettlementStatus.APPROVED,
                            Settlement.SettlementStatus.REJECTED, Settlement.SettlementStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("Bid Model")
    class BidModelTest {

        @Test
        @DisplayName("should have correct defaults")
        void shouldHaveDefaults() {
            Bid b = new Bid();
            assertThat(b.isWinning()).isFalse();
            assertThat(b.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("CommissionLedger Model")
    class CommissionLedgerModelTest {

        @Test
        @DisplayName("should set fields correctly")
        void shouldSetFields() {
            CommissionLedger cl = new CommissionLedger();
            cl.setCommissionAmount(new BigDecimal("500"));
            cl.setCommissionPercentage(new BigDecimal("5"));
            cl.setSource("AUCTION");
            assertThat(cl.getCommissionAmount()).isEqualByComparingTo("500");
            assertThat(cl.getSource()).isEqualTo("AUCTION");
        }
    }

    @Nested
    @DisplayName("AuditLog Model")
    class AuditLogModelTest {

        @Test
        @DisplayName("should set fields correctly")
        void shouldSetFields() {
            AuditLog al = new AuditLog();
            al.setAction("CREATE_CHIT");
            al.setEntityType("Chit");
            al.setEntityId(1L);
            al.setDescription("Created chit");
            assertThat(al.getAction()).isEqualTo("CREATE_CHIT");
            assertThat(al.getTimestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("ChitHistory Model")
    class ChitHistoryModelTest {

        @Test
        @DisplayName("should set fields correctly")
        void shouldSetFields() {
            ChitHistory h = new ChitHistory();
            h.setChitName("Gold");
            h.setFinalStatus("DELETED");
            h.setClosingReason("Admin cleanup");
            h.setCompleteDataJson("{}");
            assertThat(h.getChitName()).isEqualTo("Gold");
            assertThat(h.getClosedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Notification Model")
    class NotificationModelTest {

        @Test
        @DisplayName("should create notification and serialize to JSON")
        void shouldCreateAndSerialize() {
            Notification n = new Notification(
                    Notification.Type.PAYMENT_APPROVED,
                    "Payment Approved", "Your payment has been approved",
                    "user@test.com", "Gold Chit");

            assertThat(n.getId()).isNotNull();
            assertThat(n.getType()).isEqualTo(Notification.Type.PAYMENT_APPROVED);
            assertThat(n.getTitle()).isEqualTo("Payment Approved");
            assertThat(n.getTargetUser()).isEqualTo("user@test.com");
            assertThat(n.getChitName()).isEqualTo("Gold Chit");
            assertThat(n.getCreatedAt()).isNotNull();

            String json = n.toJson();
            assertThat(json).contains("\"type\":\"PAYMENT_APPROVED\"");
            assertThat(json).contains("\"title\":\"Payment Approved\"");
        }

        @Test
        @DisplayName("should support all notification types")
        void shouldSupportAllTypes() {
            assertThat(Notification.Type.values()).hasSize(17);
        }

        @Test
        @DisplayName("should handle null chitName in JSON")
        void shouldHandleNullChitName() {
            Notification n = new Notification(
                    Notification.Type.USER_UPDATED, "Updated", "details", "u@t.com", null);
            String json = n.toJson();
            assertThat(json).contains("\"chitName\":\"\"");
        }

        @Test
        @DisplayName("should escape special characters in JSON")
        void shouldEscapeSpecialChars() {
            Notification n = new Notification(
                    Notification.Type.ANNOUNCEMENT, "Title \"Test\"", "Message with \\slash",
                    "u@t.com", "Chit \"Gold\"");
            String json = n.toJson();
            assertThat(json).contains("\\\"Test\\\"");
        }
    }
}
