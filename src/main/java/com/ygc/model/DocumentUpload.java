package com.ygc.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_uploads")
@Data
@NoArgsConstructor
public class DocumentUpload {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User uploadedBy;

    @Column(nullable = false)
    private String documentName;

    @Column(nullable = false)
    private String documentType; // AGREEMENT, CERTIFICATE, PAYMENT_PROOF, IDENTIFICATION, OTHER

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String fileName;

    private Long fileSize;

    @Enumerated(EnumType.STRING)
    private DocumentStatus status = DocumentStatus.PENDING;

    @Column(name = "approval_status")
    @Enumerated(EnumType.STRING)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    private LocalDateTime uploadedAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "chit_membership_id")
    private ChitMembership chitMembership;

    @ManyToOne
    @JoinColumn(name = "payment_id")
    private Payment payment;

    // Language for document/communication (ISO 639-1 code: en, hi, ta, te, ka, etc.)
    private String language = "en";

    @PreUpdate
    public void preUpdate() {
        // Status is managed by approval workflow
    }

    public enum DocumentStatus {
        PENDING, APPROVED, REJECTED, ARCHIVED
    }

    public enum ApprovalStatus {
        PENDING, APPROVED, REJECTED, UNDER_REVIEW
    }
}

