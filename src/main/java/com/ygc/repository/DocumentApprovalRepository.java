package com.ygc.repository;

import com.ygc.model.DocumentApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentApprovalRepository extends JpaRepository<DocumentApproval, Long> {
    Optional<DocumentApproval> findByDocumentUpload_Id(Long documentUploadId);

    List<DocumentApproval> findByStatus(DocumentApproval.ApprovalStatus status);

    @Query("SELECT da FROM DocumentApproval da WHERE da.status = :status ORDER BY da.createdAt DESC")
    List<DocumentApproval> findPendingApprovals(@Param("status") DocumentApproval.ApprovalStatus status);

    List<DocumentApproval> findByApprovedBy_Id(Long adminId);

    @Query("SELECT COUNT(da) FROM DocumentApproval da WHERE da.status = :status")
    long countByStatus(@Param("status") DocumentApproval.ApprovalStatus status);
}

