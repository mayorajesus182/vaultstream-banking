-- ============================================================
-- VaultStream Customer Service - Performance Indexes
-- ============================================================
-- This migration adds optimized indexes for common query patterns.

-- Index for case-insensitive email lookups
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_customer_email_lower ON customers (LOWER(email));

-- Index for case-insensitive name search
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_customer_name_lower ON customers (
    LOWER(first_name),
    LOWER(last_name)
);

-- Index for pagination ordered by creation date
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_customer_created_at_desc ON customers (created_at DESC);

-- Partial index for active customers only (most common query)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_customer_active ON customers (id)
WHERE
    status = 'ACTIVE';

-- Composite index for status + created_at (for filtered pagination)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_customer_status_created ON customers (status, created_at DESC);