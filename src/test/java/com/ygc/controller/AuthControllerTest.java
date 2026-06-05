package com.ygc.controller;

import com.ygc.model.User;
import com.ygc.repository.UserRepository;
import com.ygc.service.UserService;
import com.ygc.util.LoggingUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserService userService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private PasswordEncoder passwordEncoder;
    @MockitoBean private LoggingUtil loggingUtil;

    @Nested
    @DisplayName("GET /login")
    class LoginPage {

        @Test
        @DisplayName("should return login page")
        void shouldReturnLoginPage() throws Exception {
            mockMvc.perform(get("/login"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("login"));
        }

        @Test
        @DisplayName("should show error message when error param present")
        void shouldShowError() throws Exception {
            mockMvc.perform(get("/login").param("error", ""))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeExists("error"));
        }

        @Test
        @DisplayName("should show logout message")
        void shouldShowLogout() throws Exception {
            mockMvc.perform(get("/login").param("logout", ""))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeExists("message"));
        }
    }

    @Nested
    @DisplayName("GET /register")
    class RegisterPage {

        @Test
        @DisplayName("should return register page")
        void shouldReturnRegisterPage() throws Exception {
            mockMvc.perform(get("/register"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("register"));
        }
    }

    @Nested
    @DisplayName("POST /register")
    class RegisterSubmit {

        @Test
        @DisplayName("should register user and redirect to login")
        void shouldRegisterAndRedirect() throws Exception {
            User newUser = new User();
            newUser.setId(1L);
            newUser.setEmail("new@test.com");
            when(userService.registerUser(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(newUser);

            mockMvc.perform(post("/register")
                            .with(csrf())
                            .param("email", "new@test.com")
                            .param("fullName", "New User")
                            .param("phone", "1234567890")
                            .param("address", "Test City"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"))
                    .andExpect(flash().attributeExists("success"));
        }

        @Test
        @DisplayName("should handle registration failure")
        void shouldHandleFailure() throws Exception {
            when(userService.registerUser(anyString(), anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("Email already exists"));

            mockMvc.perform(post("/register")
                            .with(csrf())
                            .param("email", "dup@test.com")
                            .param("fullName", "Dup User")
                            .param("phone", "123")
                            .param("address", "City"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/register"))
                    .andExpect(flash().attributeExists("error"));
        }
    }

    @Nested
    @DisplayName("GET /dashboard")
    class Dashboard {

        @Test
        @WithMockUser(username = "admin@test.com", roles = "ADMIN")
        @DisplayName("should redirect admin to admin dashboard")
        void shouldRedirectAdmin() throws Exception {
            User admin = new User();
            admin.setId(1L);
            admin.setEmail("admin@test.com");
            admin.setRole(User.Role.ADMIN);
            admin.setFirstLogin(false);
            admin.setFullName("Admin");
            when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));

            mockMvc.perform(get("/dashboard"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/dashboard"));
        }

        @Test
        @WithMockUser(username = "member@test.com", roles = "MEMBER")
        @DisplayName("should redirect member to member dashboard")
        void shouldRedirectMember() throws Exception {
            User member = new User();
            member.setId(2L);
            member.setEmail("member@test.com");
            member.setRole(User.Role.MEMBER);
            member.setFirstLogin(false);
            member.setFullName("Member");
            when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));

            mockMvc.perform(get("/dashboard"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/member/dashboard"));
        }

        @Test
        @WithMockUser(username = "first@test.com")
        @DisplayName("should redirect first-login user to change password")
        void shouldRedirectFirstLogin() throws Exception {
            User user = new User();
            user.setId(3L);
            user.setEmail("first@test.com");
            user.setFirstLogin(true);
            user.setFullName("First");
            when(userRepository.findByEmail("first@test.com")).thenReturn(Optional.of(user));

            mockMvc.perform(get("/dashboard"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/change-password"));
        }
    }

    @Nested
    @DisplayName("POST /change-password")
    class ChangePassword {

        @Test
        @WithMockUser(username = "user@test.com")
        @DisplayName("should change password when passwords match")
        void shouldChangePassword() throws Exception {
            User user = new User();
            user.setId(1L);
            user.setEmail("user@test.com");
            user.setFullName("User");
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

            mockMvc.perform(post("/change-password")
                            .with(csrf())
                            .param("newPassword", "NewPass123")
                            .param("confirmPassword", "NewPass123"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?logout"))
                    .andExpect(flash().attributeExists("success"));

            verify(userService).changePassword(user, "NewPass123");
        }

        @Test
        @WithMockUser(username = "user@test.com")
        @DisplayName("should reject when passwords don't match")
        void shouldRejectMismatch() throws Exception {
            mockMvc.perform(post("/change-password")
                            .with(csrf())
                            .param("newPassword", "Pass1")
                            .param("confirmPassword", "Pass2"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/change-password"))
                    .andExpect(flash().attributeExists("error"));

            verify(userService, never()).changePassword(any(), any());
        }
    }
}
