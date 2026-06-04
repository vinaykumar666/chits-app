package com.ygc.config;

import com.ygc.model.User;
import com.ygc.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    @DisplayName("should create admin user when not existing")
    void shouldCreateAdminWhenNotExisting() {
        when(userRepository.existsByEmail("medipalli.vinaykumar@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("Admin@123")).thenReturn("encodedAdmin");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        dataInitializer.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User admin = captor.getValue();
        assertThat(admin.getEmail()).isEqualTo("medipalli.vinaykumar@gmail.com");
        assertThat(admin.getRole()).isEqualTo(User.Role.ADMIN);
        assertThat(admin.isFirstLogin()).isFalse();
        assertThat(admin.isActive()).isTrue();
        assertThat(admin.getPassword()).isEqualTo("encodedAdmin");
    }

    @Test
    @DisplayName("should skip creation when admin already exists")
    void shouldSkipWhenAdminExists() {
        when(userRepository.existsByEmail("medipalli.vinaykumar@gmail.com")).thenReturn(true);

        dataInitializer.run();

        verify(userRepository, never()).save(any());
    }
}
