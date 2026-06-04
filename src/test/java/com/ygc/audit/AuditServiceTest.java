package com.ygc.audit;

import com.ygc.model.AuditLog;
import com.ygc.model.User;
import com.ygc.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    @Test
    @DisplayName("should create audit log with all fields")
    void shouldCreateAuditLog() {
        User user = new User();
        user.setId(1L);
        user.setEmail("admin@test.com");

        auditService.log(user, "CREATE_CHIT", "Chit", 10L, "Chit created: Gold");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getAction()).isEqualTo("CREATE_CHIT");
        assertThat(saved.getEntityType()).isEqualTo("Chit");
        assertThat(saved.getEntityId()).isEqualTo(10L);
        assertThat(saved.getDescription()).isEqualTo("Chit created: Gold");
        assertThat(saved.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("should handle null entityId gracefully")
    void shouldHandleNullEntityId() {
        User user = new User();
        user.setId(1L);

        auditService.log(user, "LOGIN", "User", null, "User logged in");

        verify(auditLogRepository).save(any(AuditLog.class));
    }
}
