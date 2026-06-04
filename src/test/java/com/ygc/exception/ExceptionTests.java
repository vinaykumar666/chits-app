package com.ygc.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ExceptionTests {

    @Test
    @DisplayName("EntityNotFoundException should carry message")
    void entityNotFound() {
        EntityNotFoundException ex = new EntityNotFoundException("Chit not found");
        assertThat(ex.getMessage()).isEqualTo("Chit not found");
    }

    @Test
    @DisplayName("DuplicateResourceException should carry message")
    void duplicateResource() {
        DuplicateResourceException ex = new DuplicateResourceException("Email already registered");
        assertThat(ex.getMessage()).isEqualTo("Email already registered");
    }

    @Test
    @DisplayName("ValidationException should carry field and message")
    void validation() {
        ValidationException ex = new ValidationException("amount", "Invalid amount");
        assertThat(ex.getMessage()).isEqualTo("Invalid amount");
        assertThat(ex.getField()).isEqualTo("amount");
    }

    @Test
    @DisplayName("AccessDeniedException should carry message")
    void accessDenied() {
        AccessDeniedException ex = new AccessDeniedException("Not authorized");
        assertThat(ex.getMessage()).isEqualTo("Not authorized");
    }
}
