package com.ygc.service;

import com.ygc.audit.AuditService;
import com.ygc.exception.DuplicateResourceException;
import com.ygc.exception.EntityNotFoundException;
import com.ygc.model.User;
import com.ygc.repository.UserRepository;
import com.ygc.util.LoggingUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private AuditService auditService;
    @Mock private LoggingUtil loggingUtil;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");
        testUser.setPhone("1234567890");
        testUser.setAddress("Test City");
        testUser.setPassword("encodedPassword");
        testUser.setRole(User.Role.MEMBER);
        testUser.setActive(true);
        testUser.setFirstLogin(true);
    }

    @Nested
    @DisplayName("registerUser")
    class RegisterUser {

        @Test
        @DisplayName("should register a new user and send email")
        void shouldRegisterNewUser() {
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(2L);
                return u;
            });

            User result = userService.registerUser("new@example.com", "New User", "9876543210", "New City");

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("new@example.com");
            assertThat(result.getFullName()).isEqualTo("New User");
            assertThat(result.isFirstLogin()).isTrue();
            assertThat(result.getRole()).isEqualTo(User.Role.MEMBER);

            verify(userRepository).save(any(User.class));
            verify(emailService).sendRegistrationConfirmation(eq("new@example.com"), eq("New User"), anyString());
            verify(auditService).log(any(), eq("REGISTER"), eq("User"), anyLong(), anyString());
        }

        @Test
        @DisplayName("should throw DuplicateResourceException for existing email")
        void shouldThrowForDuplicateEmail() {
            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.registerUser("existing@example.com", "Name", "123", "Addr"))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Email already registered");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should encode password before saving")
        void shouldEncodePassword() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedTempPass");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(3L);
                return u;
            });

            userService.registerUser("enc@test.com", "Enc User", null, null);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPassword()).isEqualTo("encodedTempPass");
        }

        @Test
        @DisplayName("should continue registration even if email sending fails")
        void shouldContinueIfEmailFails() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("enc");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(4L);
                return u;
            });
            doThrow(new RuntimeException("SMTP error")).when(emailService)
                    .sendRegistrationConfirmation(anyString(), anyString(), anyString());

            User result = userService.registerUser("email@test.com", "Name", null, null);
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("should change password and set firstLogin to false")
        void shouldChangePassword() {
            when(passwordEncoder.encode("newPass123")).thenReturn("encodedNewPass");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userService.changePassword(testUser, "newPass123");

            assertThat(testUser.getPassword()).isEqualTo("encodedNewPass");
            assertThat(testUser.isFirstLogin()).isFalse();
            verify(userRepository).save(testUser);
            verify(auditService).log(testUser, "CHANGE_PASSWORD", "User", testUser.getId(), "Password changed");
        }
    }

    @Nested
    @DisplayName("findByEmail")
    class FindByEmail {

        @Test
        @DisplayName("should find user by email")
        void shouldFindByEmail() {
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            User result = userService.findByEmail("test@example.com");
            assertThat(result).isEqualTo(testUser);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException for unknown email")
        void shouldThrowForUnknownEmail() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findByEmail("unknown@example.com"))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should find user by id")
        void shouldFindById() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            assertThat(userService.findById(1L)).isEqualTo(testUser);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException for unknown id")
        void shouldThrowForUnknownId() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> userService.findById(999L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findAllMembers")
    class FindAllMembers {

        @Test
        @DisplayName("should return only MEMBER role users")
        void shouldReturnOnlyMembers() {
            User admin = new User();
            admin.setId(10L);
            admin.setRole(User.Role.ADMIN);
            admin.setEmail("admin@test.com");
            admin.setFullName("Admin");

            User member1 = new User();
            member1.setId(11L);
            member1.setRole(User.Role.MEMBER);
            member1.setEmail("m1@test.com");
            member1.setFullName("Member1");

            User member2 = new User();
            member2.setId(12L);
            member2.setRole(User.Role.MEMBER);
            member2.setEmail("m2@test.com");
            member2.setFullName("Member2");

            when(userRepository.findAll()).thenReturn(List.of(admin, member1, member2));

            List<User> result = userService.findAllMembers();
            assertThat(result).hasSize(2);
            assertThat(result).extracting(User::getRole).containsOnly(User.Role.MEMBER);
        }

        @Test
        @DisplayName("should return empty list when no members")
        void shouldReturnEmptyWhenNoMembers() {
            User admin = new User();
            admin.setRole(User.Role.ADMIN);
            admin.setEmail("admin@test.com");
            admin.setFullName("Admin");
            when(userRepository.findAll()).thenReturn(List.of(admin));

            assertThat(userService.findAllMembers()).isEmpty();
        }
    }
}
