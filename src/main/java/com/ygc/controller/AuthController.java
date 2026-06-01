package com.ygc.controller;

import com.ygc.model.User;
import com.ygc.repository.UserRepository;
import com.ygc.service.UserService;
import com.ygc.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoggingUtil loggingUtil;

    @GetMapping("/")
    public String home(Authentication auth) {
        try {
            if (auth != null && auth.isAuthenticated()) {
                loggingUtil.debug("Authenticated user redirecting to dashboard", "AuthController.home");
                return "redirect:/dashboard";
            }
            loggingUtil.debug("Unauthenticated user redirecting to login", "AuthController.home");
            return "redirect:/login";
        } catch (Exception e) {
            loggingUtil.error("Error in home endpoint", "AuthController.home", e);
            return "redirect:/login";
        }
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        try {
            if (error != null) {
                loggingUtil.warn("Login error parameter present", "AuthController.loginPage");
                model.addAttribute("error", "Invalid email or password");
            }
            if (logout != null) {
                loggingUtil.info("User logged out", "AuthController.loginPage");
                model.addAttribute("message", "Logged out successfully");
            }
            return "login";
        } catch (Exception e) {
            loggingUtil.error("Error in login page", "AuthController.loginPage", e);
            return "login";
        }
    }

    @GetMapping("/register")
    public String registerPage() {
        try {
            loggingUtil.debug("Register page accessed", "AuthController.registerPage");
            return "register";
        } catch (Exception e) {
            loggingUtil.error("Error loading register page", "AuthController.registerPage", e);
            return "register";
        }
    }

    @PostMapping("/register")
    public String register(@RequestParam String email,
                           @RequestParam String fullName,
                           @RequestParam String phone,
                           @RequestParam String address,
                           RedirectAttributes redirectAttributes) {
        try {
            loggingUtil.debug("User registration request for email: " + email, "AuthController.register");

            userService.registerUser(email, fullName, phone, address);

            loggingUtil.info("User registration successful for email: " + email, "AuthController.register");
            redirectAttributes.addFlashAttribute("success",
                    "Registration successful! Check your email for temporary password.");
            return "redirect:/login";
        } catch (Exception e) {
            loggingUtil.error("User registration failed", "AuthController.register", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        try {
            loggingUtil.debug("Dashboard access attempt for user: " + auth.getName(), "AuthController.dashboard");

            User user = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> {
                        loggingUtil.warn("User not found in database: " + auth.getName(), "AuthController.dashboard");
                        return new RuntimeException("User not found");
                    });

            if (user.isFirstLogin()) {
                loggingUtil.info("First time login, redirecting to change password for user: " + auth.getName(), "AuthController.dashboard");
                return "redirect:/change-password";
            }

            model.addAttribute("user", user);

            if (user.getRole() == User.Role.ADMIN) {
                loggingUtil.userAction(auth.getName(), "DASHBOARD_ACCESS_ADMIN", "AuthController.dashboard");
                return "redirect:/admin/dashboard";
            }

            loggingUtil.userAction(auth.getName(), "DASHBOARD_ACCESS_MEMBER", "AuthController.dashboard");
            return "redirect:/member/dashboard";
        } catch (Exception e) {
            loggingUtil.error("Error accessing dashboard", "AuthController.dashboard", e);
            return "redirect:/login?error=session_error";
        }
    }

    @GetMapping("/change-password")
    public String changePasswordPage() {
        try {
            loggingUtil.debug("Change password page accessed", "AuthController.changePasswordPage");
            return "change-password";
        } catch (Exception e) {
            loggingUtil.error("Error loading change password page", "AuthController.changePasswordPage", e);
            return "change-password";
        }
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        try {
            loggingUtil.debug("Password change request for user: " + auth.getName(), "AuthController.changePassword");

            if (!newPassword.equals(confirmPassword)) {
                loggingUtil.validationError("password_confirmation", "Passwords do not match", "AuthController.changePassword");
                redirectAttributes.addFlashAttribute("error", "Passwords do not match");
                return "redirect:/change-password";
            }

            User user = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> {
                        loggingUtil.warn("User not found: " + auth.getName(), "AuthController.changePassword");
                        return new RuntimeException("User not found");
                    });

            userService.changePassword(user, newPassword);

            loggingUtil.info("Password changed successfully for user: " + auth.getName(), "AuthController.changePassword");
            redirectAttributes.addFlashAttribute("success", "Password changed. Please login again.");
            return "redirect:/login?logout";
        } catch (Exception e) {
            loggingUtil.error("Error changing password", "AuthController.changePassword", e);
            redirectAttributes.addFlashAttribute("error", "Error changing password: " + e.getMessage());
            return "redirect:/change-password";
        }
    }
}
