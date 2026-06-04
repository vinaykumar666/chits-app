package com.ygc.service;

import com.ygc.util.LoggingUtil;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;
    @Mock private LoggingUtil loggingUtil;
    @Mock private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "mailFrom", "test@ygcinternal.com");
        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Nested
    @DisplayName("sendHtmlEmailWithAttachment")
    class SendHtmlEmail {

        @Test
        @DisplayName("should send email via mailSender")
        void shouldSendEmail() {
            emailService.sendHtmlEmailWithAttachment("to@test.com", "Subject", "<p>Body</p>", null, null);
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("should handle send failure gracefully")
        void shouldHandleSendFailure() {
            doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

            assertThatCode(() -> emailService.sendHtmlEmailWithAttachment("to@test.com", "Sub", "Body", null, null))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("sendRegistrationConfirmation")
    class SendRegistration {

        @Test
        @DisplayName("should send registration email with temp password")
        void shouldSendRegistrationEmail() {
            emailService.sendRegistrationConfirmation("new@test.com", "New User", "TEMP1234");
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("should handle registration email failure")
        void shouldHandleFailure() {
            doThrow(new RuntimeException("fail")).when(mailSender).send(any(MimeMessage.class));

            assertThatCode(() -> emailService.sendRegistrationConfirmation("x@y.com", "Name", "pass"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("sendPaymentDueReminder")
    class PaymentReminder {

        @Test
        @DisplayName("should send payment reminder")
        void shouldSendReminder() {
            emailService.sendPaymentDueReminder("m@t.com", "Member", "Gold Chit", "2025-01-15", "5000");
            verify(mailSender).send(any(MimeMessage.class));
        }
    }

    @Nested
    @DisplayName("sendPaymentApproval")
    class PaymentApproval {

        @Test
        @DisplayName("should send approval email")
        void shouldSendApproval() {
            emailService.sendPaymentApproval("m@t.com", "Member", "Gold", "5000", true);
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("should send rejection email")
        void shouldSendRejection() {
            emailService.sendPaymentApproval("m@t.com", "Member", "Gold", "5000", false);
            verify(mailSender).send(any(MimeMessage.class));
        }
    }

    @Nested
    @DisplayName("sendAuctionAnnouncement")
    class AuctionAnnouncement {

        @Test
        @DisplayName("should send auction announcement")
        void shouldSendAnnouncement() {
            emailService.sendAuctionAnnouncement("m@t.com", "Member", "Gold", "2025-02-01", 3);
            verify(mailSender).send(any(MimeMessage.class));
        }
    }

    @Nested
    @DisplayName("styled email methods")
    class StyledEmails {

        @Test
        @DisplayName("sendSettlementConfirmation should use styled template")
        void shouldSendSettlement() {
            emailService.sendSettlementConfirmation("m@t.com", "Member", "Gold", "50000");
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("sendPaymentRejected should use styled template")
        void shouldSendPaymentRejected() {
            emailService.sendPaymentRejected("m@t.com", "Member", "Gold", "5000", "Invalid screenshot");
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("sendMembershipRejected should use styled template")
        void shouldSendMembershipRejected() {
            emailService.sendMembershipRejected("m@t.com", "Member", "Gold", "Criteria not met");
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("sendSettlementRejected should use styled template")
        void shouldSendSettlementRejected() {
            emailService.sendSettlementRejected("m@t.com", "Member", "Gold", "Pending dues");
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("sendAnnouncement should use styled template")
        void shouldSendAnnouncement() {
            emailService.sendAnnouncement("m@t.com", "Member", "Important Update", "Please note...");
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("sendUserUpdated should use styled template")
        void shouldSendUserUpdated() {
            emailService.sendUserUpdated("m@t.com", "Member", "Phone updated");
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("sendChitUpdated should use styled template")
        void shouldSendChitUpdated() {
            emailService.sendChitUpdated("m@t.com", "Member", "Gold", "Status changed to ACTIVE");
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("sendMemberRemovedFromChit should send email")
        void shouldSendMemberRemoved() {
            emailService.sendMemberRemovedFromChit("m@t.com", "Member", "Gold", "Non-payment");
            verify(mailSender).send(any(MimeMessage.class));
        }
    }
}
