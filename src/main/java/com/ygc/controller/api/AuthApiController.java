package com.ygc.controller.api;

import com.ygc.dto.*;
import com.ygc.exception.ValidationException;
import com.ygc.model.User;
import com.ygc.repository.UserRepository;
import com.ygc.security.JwtUtil;
import com.ygc.security.UserDetailsServiceImpl;
import com.ygc.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final UserService userService;
    private final DtoMapper dtoMapper;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new ValidationException("credentials", "Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        if (!user.isActive()) {
            throw new ValidationException("account", "Account is inactive");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        return ResponseEntity.ok(AuthResponse.builder()
                .accessToken(jwtUtil.generateToken(userDetails))
                .refreshToken(jwtUtil.generateRefreshToken(userDetails))
                .user(dtoMapper.toUserDto(user))
                .build());
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.registerUser(
                request.getEmail(), request.getFullName(), request.getPhone(), request.getAddress());
        return ResponseEntity.ok(Map.of(
                "message", "Registration successful! Check your email for temporary password.",
                "user", dtoMapper.toUserDto(user)));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        return ResponseEntity.ok(dtoMapper.toUserDto(user));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request, Authentication auth) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ValidationException("confirmPassword", "Passwords do not match");
        }
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        userService.changePassword(user, request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ValidationException("refreshToken", "Refresh token is required");
        }
        if (!jwtUtil.isRefreshToken(refreshToken)) {
            throw new ValidationException("refreshToken", "Invalid refresh token");
        }

        String email = jwtUtil.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        if (!jwtUtil.validateRefreshToken(refreshToken, userDetails)) {
            throw new ValidationException("refreshToken", "Refresh token expired or invalid");
        }

        User user = userRepository.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(AuthResponse.builder()
                .accessToken(jwtUtil.generateToken(userDetails))
                .refreshToken(jwtUtil.generateRefreshToken(userDetails))
                .user(dtoMapper.toUserDto(user))
                .build());
    }
}
