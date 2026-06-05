package com.ygc.repository;

import com.ygc.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryTests {

    @Autowired private TestEntityManager em;
    @Autowired private UserRepository userRepository;
    @Autowired private ChitRepository chitRepository;
    @Autowired private ChitMembershipRepository membershipRepository;
    @Autowired private CommissionLedgerRepository commissionLedgerRepository;

    private User admin;
    private User member;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setEmail("admin@test.com");
        admin.setPassword("encoded");
        admin.setFullName("Admin User");
        admin.setRole(User.Role.ADMIN);
        admin = em.persistAndFlush(admin);

        member = new User();
        member.setEmail("member@test.com");
        member.setPassword("encoded");
        member.setFullName("Member User");
        member.setRole(User.Role.MEMBER);
        member = em.persistAndFlush(member);
    }

    @Nested
    @DisplayName("UserRepository")
    class UserRepoTests {

        @Test
        @DisplayName("findByEmail should find existing user")
        void findByEmail() {
            Optional<User> found = userRepository.findByEmail("admin@test.com");
            assertThat(found).isPresent();
            assertThat(found.get().getFullName()).isEqualTo("Admin User");
        }

        @Test
        @DisplayName("findByEmail should return empty for unknown")
        void findByEmailNotFound() {
            assertThat(userRepository.findByEmail("nobody@test.com")).isEmpty();
        }

        @Test
        @DisplayName("existsByEmail should return true for existing")
        void existsByEmail() {
            assertThat(userRepository.existsByEmail("member@test.com")).isTrue();
        }

        @Test
        @DisplayName("existsByEmail should return false for unknown")
        void notExistsByEmail() {
            assertThat(userRepository.existsByEmail("nobody@test.com")).isFalse();
        }
    }

    @Nested
    @DisplayName("ChitRepository")
    class ChitRepoTests {

        @Test
        @DisplayName("findByStatus should filter correctly")
        void findByStatus() {
            Chit openChit = createChit("Open Chit", Chit.ChitStatus.OPEN);
            Chit activeChit = createChit("Active Chit", Chit.ChitStatus.ACTIVE);

            List<Chit> openChits = chitRepository.findByStatus(Chit.ChitStatus.OPEN);
            assertThat(openChits).hasSize(1);
            assertThat(openChits.get(0).getName()).isEqualTo("Open Chit");
        }

        @Test
        @DisplayName("findAvailableChits should return open chits with available spots")
        void findAvailableChits() {
            Chit chit = createChit("Available", Chit.ChitStatus.OPEN);
            List<Chit> available = chitRepository.findAvailableChits();
            assertThat(available).isNotEmpty();
        }

        @Test
        @DisplayName("cascading delete should remove commission ledger entries")
        void cascadeDeleteShouldRemoveCommissionLedger() {
            Chit chit = createChit("Cascade Test", Chit.ChitStatus.CANCELLED);
            em.flush();

            CommissionLedger cl = new CommissionLedger();
            cl.setChit(chit);
            cl.setCommissionAmount(new BigDecimal("500"));
            cl.setCommissionPercentage(new BigDecimal("5"));
            cl.setSource("AUCTION");
            em.persistAndFlush(cl);

            Long chitId = chit.getId();
            assertThat(commissionLedgerRepository.findByChit(chit)).hasSize(1);

            // Delete the chit — should cascade to commission_ledger
            chitRepository.delete(chit);
            chitRepository.flush();
            em.clear();

            assertThat(chitRepository.findById(chitId)).isEmpty();
            assertThat(commissionLedgerRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("cascading delete should remove memberships")
        void cascadeDeleteShouldRemoveMemberships() {
            Chit chit = createChit("Member Cascade", Chit.ChitStatus.OPEN);
            em.flush();

            ChitMembership m = new ChitMembership();
            m.setChit(chit);
            m.setUser(member);
            m.setStatus(ChitMembership.MembershipStatus.ACTIVE);
            em.persistAndFlush(m);

            chitRepository.delete(chit);
            chitRepository.flush();
            em.clear();

            assertThat(membershipRepository.findAll()).isEmpty();
        }
    }

    @Nested
    @DisplayName("ChitMembershipRepository")
    class MembershipRepoTests {

        @Test
        @DisplayName("existsByChitAndUser should work correctly")
        void existsByChitAndUser() {
            Chit chit = createChit("Exist Test", Chit.ChitStatus.OPEN);
            ChitMembership m = new ChitMembership();
            m.setChit(chit);
            m.setUser(member);
            em.persistAndFlush(m);

            assertThat(membershipRepository.existsByChitAndUser(chit, member)).isTrue();
            assertThat(membershipRepository.existsByChitAndUser(chit, admin)).isFalse();
        }

        @Test
        @DisplayName("findByUser should return user memberships")
        void findByUser() {
            Chit chit = createChit("User Chit", Chit.ChitStatus.OPEN);
            ChitMembership m = new ChitMembership();
            m.setChit(chit);
            m.setUser(member);
            em.persistAndFlush(m);

            List<ChitMembership> memberships = membershipRepository.findByUser(member);
            assertThat(memberships).hasSize(1);
        }

        @Test
        @DisplayName("findByChit should return chit memberships")
        void findByChit() {
            Chit chit = createChit("Chit Members", Chit.ChitStatus.OPEN);
            ChitMembership m = new ChitMembership();
            m.setChit(chit);
            m.setUser(member);
            em.persistAndFlush(m);

            List<ChitMembership> memberships = membershipRepository.findByChit(chit);
            assertThat(memberships).hasSize(1);
        }

        @Test
        @DisplayName("countByChitAndStatusNot should exclude given status")
        void countByChitAndStatusNot() {
            Chit chit = createChit("Count Test", Chit.ChitStatus.OPEN);

            ChitMembership active = new ChitMembership();
            active.setChit(chit);
            active.setUser(member);
            active.setStatus(ChitMembership.MembershipStatus.ACTIVE);
            em.persistAndFlush(active);

            User member2 = new User();
            member2.setEmail("m2@test.com");
            member2.setPassword("enc");
            member2.setFullName("M2");
            member2 = em.persistAndFlush(member2);

            ChitMembership exited = new ChitMembership();
            exited.setChit(chit);
            exited.setUser(member2);
            exited.setStatus(ChitMembership.MembershipStatus.EXITED);
            em.persistAndFlush(exited);

            long count = membershipRepository.countByChitAndStatusNot(chit, ChitMembership.MembershipStatus.EXITED);
            assertThat(count).isEqualTo(1);
        }
    }

    private Chit createChit(String name, Chit.ChitStatus status) {
        Chit chit = new Chit();
        chit.setName(name);
        chit.setDescription("Test chit");
        chit.setMonthlyAmount(new BigDecimal("5000"));
        chit.setTotalMembers(10);
        chit.setDurationMonths(12);
        chit.setAdminCommissionPercentage(new BigDecimal("5"));
        chit.setStartDate(LocalDate.now().plusDays(30));
        chit.setEndDate(LocalDate.now().plusMonths(12));
        chit.setTotalChitValue(new BigDecimal("50000"));
        chit.setStatus(status);
        chit.setCreatedBy(admin);
        return em.persistAndFlush(chit);
    }
}
