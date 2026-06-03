package com.ygc.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Persists a complete snapshot of a chit (as JSON) when the chit is
 * CANCELLED, COMPLETED, or deleted by the admin.
 *
 * Table: chit_history
 */
@Entity
@Table(name = "chit_history")
@Data
@NoArgsConstructor
public class ChitHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Original chit PK (may be null if the chit row was hard-deleted) */
    private Long originalChitId;

    @Column(nullable = false)
    private String chitName;

    /** CANCELLED / COMPLETED / DELETED */
    @Column(nullable = false)
    private String finalStatus;

    /** Admin-supplied reason for closing/deleting */
    @Column(columnDefinition = "TEXT")
    private String closingReason;

    /** Admin who triggered the close/delete */
    private String closedByEmail;
    private String closedByName;

    /** Full snapshot: chit + members + payments + auctions + commissions + settlements */
    @Lob
    @Column(name = "complete_data_json", columnDefinition = "TEXT", nullable = false)
    private String completeDataJson;

    /** Path of the generated analysis PDF (null if PDF generation failed) */
    private String analysisPdfPath;

    @Column(nullable = false)
    private LocalDateTime closedAt = LocalDateTime.now();
}
