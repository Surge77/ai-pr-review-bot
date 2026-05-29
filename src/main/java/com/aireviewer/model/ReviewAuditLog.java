package com.aireviewer.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Audit record persisted once per file evaluated during a review (and once per
 * pipeline-level failure). Maps to the {@code review_audit_log} table created by
 * Flyway V1.
 */
@Entity
@Table(name = "review_audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewAuditLog {

    /** Sentinel {@code file_path} for failures that are not tied to a single file. */
    public static final String PIPELINE_LEVEL = "*";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_id")
    private String deliveryId;

    @Column(name = "pr_number", nullable = false)
    private int prNumber;

    @Column(name = "repo_full_name", nullable = false)
    private String repoFullName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "commit_sha")
    private String commitSha;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReviewStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "llm_feedback")
    private String llmFeedback;

    @Column(name = "issues_found", nullable = false)
    private int issuesFound;

    @Column(name = "has_critical", nullable = false)
    private boolean hasCritical;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
