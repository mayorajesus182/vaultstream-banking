-- ============================================================
-- V2: Add suspension_reason column
-- ============================================================

-- Add suspension_reason column to customers table
ALTER TABLE customers ADD COLUMN suspension_reason VARCHAR(500);

-- Add index for querying suspended customers with reasons
CREATE INDEX idx_customer_suspension ON customers (status, suspension_reason)
WHERE
    status = 'SUSPENDED';

-- Add comment
COMMENT ON COLUMN customers.suspension_reason IS 'Reason for customer suspension (required when status is SUSPENDED)';