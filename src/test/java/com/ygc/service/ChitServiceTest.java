package com.ygc.service;

import com.ygc.audit.AuditService;
import com.ygc.exception.EntityNotFoundException;
import com.ygc.model.*;
import com.ygc.repository.*;
import com.ygc.util.LoggingUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChitServiceTest {

    @Mock private ChitRepository chitRepository;
    @Mock private ChitMembershipRepository membershipRepository;
    @Mock private AuditService auditService;
    @Mock private EmailService emailService;
    @Mock private ChitAgreementService agreementService;
    @Mock private LoggingUtil loggingUtil;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private ChitService chitService;

    private User admin;
    private User member;
    private Chit testChit;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@test.com");
        admin.setFullName("Admin");
        admin.setRole(User.Role.ADMIN);

        member = new User();
        member.setId(2L);
        member.setEmail("member@test.com");
        member.setFullName("Member");
        member.setRole(User.Role.MEMBER);

        testChit = new Chit();
        testChit.setId(1L);
        testChit.setName("Gold Chit 2024");
        testChit.setDescription("Monthly gold saving chit");
        testChit.setMonthlyAmount(new BigDecimal("5000"));
        testChit.setTotalMembers(10);
        testChit.setDurationMonths(12);
        testChit.setAdminCommissionPercentage(new BigDecimal("5"));
        testChit.setStartDate(LocalDate.now().plusDays(30));
        testChit.setStatus(Chit.ChitStatus.OPEN);
    }

    @Nested
    @DisplayName("createChit")
    class CreateChit {

        @Test
        @DisplayName("should create chit with computed total value")
        void shouldCreateChitWithTotalValue() {
            when(chitRepository.save(any(Chit.class))).thenAnswer(inv -> {
                Chit c = inv.getArgument(0);
                c.setId(10L);
                return c;
            });

            Chit result = chitService.createChit(testChit, admin);

            assertThat(result.getTotalChitValue()).isEqualByComparingTo("50000");
            assertThat(result.getCreatedBy()).isEqualTo(admin);
            assertThat(result.getEndDate()).isNotNull();
            verify(chitRepository).save(testChit);
            verify(auditService).log(eq(admin), eq("CREATE_CHIT"), eq("Chit"), anyLong(), anyString());
        }

        @Test
        @DisplayName("should set end date based on start date + duration")
        void shouldSetEndDate() {
            LocalDate start = LocalDate.of(2025, 1, 1);
            testChit.setStartDate(start);
            testChit.setDurationMonths(12);

            when(chitRepository.save(any(Chit.class))).thenAnswer(inv -> {
                Chit c = inv.getArgument(0);
                c.setId(11L);
                return c;
            });

            Chit result = chitService.createChit(testChit, admin);
            assertThat(result.getEndDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        }
    }

    @Nested
    @DisplayName("requestJoin")
    class RequestJoin {

        @Test
        @DisplayName("should create pending membership for valid request")
        void shouldCreatePendingMembership() {
            when(chitRepository.findById(1L)).thenReturn(Optional.of(testChit));
            when(membershipRepository.existsByChitAndUser(testChit, member)).thenReturn(false);
            when(membershipRepository.countByChitAndStatusNot(testChit, ChitMembership.MembershipStatus.EXITED)).thenReturn(5L);
            when(membershipRepository.save(any(ChitMembership.class))).thenAnswer(inv -> {
                ChitMembership m = inv.getArgument(0);
                m.setId(100L);
                return m;
            });

            ChitMembership result = chitService.requestJoin(1L, member, true, true, true);

            assertThat(result.getStatus()).isEqualTo(ChitMembership.MembershipStatus.PENDING);
            assertThat(result.getChit()).isEqualTo(testChit);
            assertThat(result.getUser()).isEqualTo(member);
            verify(agreementService).recordAgreementAcceptance(any(), eq(true), eq(true), eq(true));
        }

        @Test
        @DisplayName("should throw when chit is not open")
        void shouldThrowWhenChitNotOpen() {
            testChit.setStatus(Chit.ChitStatus.ACTIVE);
            when(chitRepository.findById(1L)).thenReturn(Optional.of(testChit));

            assertThatThrownBy(() -> chitService.requestJoin(1L, member, true, true, true))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not open");
        }

        @Test
        @DisplayName("should throw when chit has already started")
        void shouldThrowWhenChitStarted() {
            testChit.setStartDate(LocalDate.now().minusDays(1));
            when(chitRepository.findById(1L)).thenReturn(Optional.of(testChit));

            assertThatThrownBy(() -> chitService.requestJoin(1L, member, true, true, true))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already started");
        }

        @Test
        @DisplayName("should throw when user already a member")
        void shouldThrowWhenAlreadyMember() {
            when(chitRepository.findById(1L)).thenReturn(Optional.of(testChit));
            when(membershipRepository.existsByChitAndUser(testChit, member)).thenReturn(true);

            assertThatThrownBy(() -> chitService.requestJoin(1L, member, true, true, true))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Already requested");
        }

        @Test
        @DisplayName("should throw when chit is full")
        void shouldThrowWhenChitFull() {
            when(chitRepository.findById(1L)).thenReturn(Optional.of(testChit));
            when(membershipRepository.existsByChitAndUser(testChit, member)).thenReturn(false);
            when(membershipRepository.countByChitAndStatusNot(testChit, ChitMembership.MembershipStatus.EXITED))
                    .thenReturn(10L); // equal to totalMembers

            assertThatThrownBy(() -> chitService.requestJoin(1L, member, true, true, true))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("full");
        }

        @Test
        @DisplayName("should throw when chit not found")
        void shouldThrowWhenChitNotFound() {
            when(chitRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chitService.requestJoin(999L, member, true, true, true))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("approveMembership")
    class ApproveMembership {

        @Test
        @DisplayName("should approve membership and generate agreement")
        void shouldApproveMembership() {
            ChitMembership membership = new ChitMembership();
            membership.setId(100L);
            membership.setChit(testChit);
            membership.setUser(member);
            membership.setStatus(ChitMembership.MembershipStatus.PENDING);

            when(membershipRepository.findById(100L)).thenReturn(Optional.of(membership));
            when(membershipRepository.save(any())).thenReturn(membership);

            chitService.approveMembership(100L, admin);

            assertThat(membership.getStatus()).isEqualTo(ChitMembership.MembershipStatus.ACTIVE);
            verify(agreementService).generateAndDistributeAgreementPdf(membership, admin);
            verify(notificationService).notifyChitRegistrationApproved(
                    member.getEmail(), member.getFullName(), testChit.getName());
        }

        @Test
        @DisplayName("should continue even if PDF generation fails")
        void shouldContinueIfPdfFails() {
            ChitMembership membership = new ChitMembership();
            membership.setId(101L);
            membership.setChit(testChit);
            membership.setUser(member);
            membership.setStatus(ChitMembership.MembershipStatus.PENDING);

            when(membershipRepository.findById(101L)).thenReturn(Optional.of(membership));
            when(membershipRepository.save(any())).thenReturn(membership);
            doThrow(new RuntimeException("PDF error")).when(agreementService)
                    .generateAndDistributeAgreementPdf(any(), any());

            assertThatCode(() -> chitService.approveMembership(101L, admin))
                    .doesNotThrowAnyException();
            assertThat(membership.getStatus()).isEqualTo(ChitMembership.MembershipStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("query methods")
    class QueryMethods {

        @Test
        @DisplayName("getAllChits should delegate to repository")
        void shouldGetAllChits() {
            when(chitRepository.findAll()).thenReturn(List.of(testChit));
            assertThat(chitService.getAllChits()).hasSize(1);
        }

        @Test
        @DisplayName("getAvailableChits should delegate to repository")
        void shouldGetAvailableChits() {
            when(chitRepository.findAvailableChits()).thenReturn(List.of(testChit));
            assertThat(chitService.getAvailableChits()).hasSize(1);
        }

        @Test
        @DisplayName("findById should return chit")
        void shouldFindById() {
            when(chitRepository.findById(1L)).thenReturn(Optional.of(testChit));
            assertThat(chitService.findById(1L)).isEqualTo(testChit);
        }

        @Test
        @DisplayName("findById should throw for missing chit")
        void shouldThrowForMissingChit() {
            when(chitRepository.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> chitService.findById(999L))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("getMembershipsForUser should delegate to repository")
        void shouldGetMembershipsForUser() {
            when(membershipRepository.findByUser(member)).thenReturn(List.of());
            assertThat(chitService.getMembershipsForUser(member)).isEmpty();
        }

        @Test
        @DisplayName("getMembershipsForChit should delegate to repository")
        void shouldGetMembershipsForChit() {
            when(membershipRepository.findByChit(testChit)).thenReturn(List.of());
            assertThat(chitService.getMembershipsForChit(testChit)).isEmpty();
        }
    }
}
