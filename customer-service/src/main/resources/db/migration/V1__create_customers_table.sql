-- ============================================================
-- V1: Create customers table
-- ============================================================

CREATE TABLE customers (
    id UUID PRIMARY KEY,
    customer_number VARCHAR(30) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone_number VARCHAR(20),
    date_of_birth DATE,
    national_id VARCHAR(50) NOT NULL UNIQUE,

-- Address fields
street VARCHAR(255),
street_number VARCHAR(20),
apartment VARCHAR(20),
city VARCHAR(100),
state VARCHAR(100),
postal_code VARCHAR(20),
country VARCHAR(100),

-- Status
status VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION',
type VARCHAR(20) NOT NULL DEFAULT 'INDIVIDUAL',

-- Timestamps
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

-- Optimistic locking
version INT NOT NULL DEFAULT 0 );

-- Indexes
CREATE INDEX idx_customer_status ON customers (status);

CREATE INDEX idx_customer_name ON customers (first_name, last_name);

CREATE INDEX idx_customer_created ON customers (created_at);

-- Comments
COMMENT ON TABLE customers IS 'Bank customers information';

COMMENT ON COLUMN customers.customer_number IS 'Unique business identifier for the customer';

COMMENT ON COLUMN customers.national_id IS 'Government-issued identification number';

COMMENT ON COLUMN customers.status IS 'PENDING_VERIFICATION, ACTIVE, SUSPENDED, INACTIVE';

COMMENT ON COLUMN customers.type IS 'INDIVIDUAL, BUSINESS, PREMIUM';