-- ============================================================================
--  V1 - initial schema
--  review_audit_log: one row per file evaluated during a PR review.
--  status REVIEWED  = file sent to the LLM and feedback recorded
--         SKIPPED   = unchanged file served from cache (no LLM call)
--         FAILED    = LLM or pipeline error for this file
-- ============================================================================
CREATE TABLE review_audit_log (
    id             BIGSERIAL    PRIMARY KEY,
    delivery_id    VARCHAR(128),                       -- GitHub X-GitHub-Delivery (idempotency)
    pr_number      INTEGER      NOT NULL,
    repo_full_name VARCHAR(255) NOT NULL,              -- "owner/repo"
    file_path      VARCHAR(1024) NOT NULL,
    commit_sha     VARCHAR(64),                        -- head SHA the review ran against
    status         VARCHAR(16)  NOT NULL,
    llm_feedback   JSONB,                              -- full ReviewFeedback payload
    issues_found   INTEGER      NOT NULL DEFAULT 0,
    has_critical   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_status CHECK (status IN ('REVIEWED', 'SKIPPED', 'FAILED'))
);

-- Reporting query: list/paginate reviews for a repo, newest first.
CREATE INDEX idx_audit_repo_created ON review_audit_log (repo_full_name, created_at DESC);

-- Reporting query: fetch all files for a specific PR within a repo.
CREATE INDEX idx_audit_repo_pr ON review_audit_log (repo_full_name, pr_number);

-- Stats query: skip-rate / status aggregation per repo.
CREATE INDEX idx_audit_repo_status ON review_audit_log (repo_full_name, status);
