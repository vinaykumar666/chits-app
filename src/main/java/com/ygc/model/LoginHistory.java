package com.ygc.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "login_history")
@Data
@NoArgsConstructor
public class LoginHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String ipAddress;
    private String userAgent;
    private String deviceFingerprint;
    private String geoLocation; // "lat,lng" or city name from IP
    private boolean success;
    private String failureReason;
    private LocalDateTime loginAt = LocalDateTime.now();

    public LoginHistory(User user, String ip, String ua, boolean success) {
        this.user = user;
        this.ipAddress = ip;
        this.userAgent = ua;
        this.success = success;
    }
}
