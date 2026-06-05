package com.ygc.repository;

import com.ygc.model.DocumentUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentUploadRepository extends JpaRepository<DocumentUpload, Long> {
    List<DocumentUpload> findByStatus(DocumentUpload.DocumentStatus status);

    List<DocumentUpload> findByApprovalStatus(DocumentUpload.ApprovalStatus approvalStatus);

    List<DocumentUpload> findByChitMembership_Id(Long membershipId);

    List<DocumentUpload> findByPayment_Id(Long paymentId);

    List<DocumentUpload> findByUploadedBy_Id(Long userId);

    @Query("SELECT d FROM DocumentUpload d WHERE d.approvalStatus = :status ORDER BY d.uploadedAt DESC")
    List<DocumentUpload> findDocumentsPendingApproval(@Param("status") DocumentUpload.ApprovalStatus status);

    @Query("SELECT d FROM DocumentUpload d WHERE d.documentType = :type AND d.approvalStatus = :status")
    List<DocumentUpload> findByDocumentTypeAndApprovalStatus(
            @Param("type") String documentType,
            @Param("status") DocumentUpload.ApprovalStatus approvalStatus);

    List<DocumentUpload> findByLanguage(String language);
}

