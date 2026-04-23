-- Create Users Table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

-- Create Issues Table
CREATE TABLE issues (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id BIGINT NOT NULL,
    assignee_id BIGINT,
    is_updated BOOLEAN DEFAULT FALSE,
    severity SMALLINT, -- Stored as Ordinal (0, 1, 2, 3)
    cvss_score DOUBLE PRECISION,
    cvss_vector VARCHAR(100),
    cwe_id VARCHAR(20),
    cve_id VARCHAR(20),
    status VARCHAR(50) DEFAULT 'OPEN',
    scanner_source VARCHAR(100),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_owner FOREIGN KEY (owner_id) REFERENCES users(id),
    CONSTRAINT fk_assignee FOREIGN KEY (assignee_id) REFERENCES users(id)
);

-- Create Audit Logs Table
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    principal VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    resource VARCHAR(100) NOT NULL,
    resource_id VARCHAR(100),
    status VARCHAR(50) NOT NULL
);