package com.ygc.dto;

import com.ygc.model.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserDto {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String address;
    private User.Role role;
    private boolean firstLogin;
    private boolean active;
    private boolean termsAccepted;
    private LocalDateTime createdAt;
}
