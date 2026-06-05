package com.ygc.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_approvals")
@Data
@NoArgsConstructor
public class DocumentApproval {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "document_upload_id", nullable = false)
    private DocumentUpload documentUpload;

    @ManyToOne
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    private String approvalComments;

    private String rejectionReason;

    private LocalDateTime approvedAt;

    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    // Approval workflow
    private boolean sentForReview = false;
    private LocalDateTime sentForReviewAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum ApprovalStatus {
        PENDING, UNDER_REVIEW, APPROVED, REJECTED, HOLD
    }
}

