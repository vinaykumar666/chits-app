package com.ygc.controller;

import com.ygc.model.User;
import com.ygc.repository.UserRepository;
import com.ygc.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/")
    public String home(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/dashboard";
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (error != null) model.addAttribute("error", "Invalid email or password");
        if (logout != null) model.addAttribute("message", "Logged out successfully");
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String email,
                           @RequestParam String fullName,
                           @RequestParam String phone,
                           @RequestParam String address,
                           RedirectAttributes redirectAttributes) {
        try {
            userService.registerUser(email, fullName, phone, address);
            redirectAttributes.addFlashAttribute("success",
                    "Registration successful! Check your email for temporary password.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        if (user.isFirstLogin()) return "redirect:/change-password";
        model.addAttribute("user", user);
        if (user.getRole() == User.Role.ADMIN) return "redirect:/admin/dashboard";
        return "redirect:/member/dashboard";
    }

    @GetMapping("/change-password")
    public String changePasswordPage() {
        return "change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/change-password";
        }
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        userService.changePassword(user, newPassword);
        redirectAttributes.addFlashAttribute("success", "Password changed. Please login again.");
        return "redirect:/login?logout";
    }
}
