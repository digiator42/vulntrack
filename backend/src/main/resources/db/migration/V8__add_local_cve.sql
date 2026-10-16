-- Local CVE cache table.
-- Required by NvdClientService2/NvdSyncService (local_cve). Locally this
-- table was auto-created by JPA ddl-auto; in a container/prod profile
-- there is no such safety net, so dependency scans fail with
-- 'relation "local_cve" does not exist'. Column names/types mirror the
-- com.vulntrack.model.LocalCve entity under Hibernate's
-- CamelCase-to-snake_case naming strategy.
CREATE TABLE local_cve (
    id BIGSERIAL PRIMARY KEY,
    cve_id VARCHAR(255) NOT NULL,
    cpe_product TEXT,
    cpe_vendor TEXT,
    description TEXT,
    cvss_score DOUBLE PRECISION,
    cvss_version VARCHAR(10),
    severity VARCHAR(255),
    vector_string VARCHAR(255),
    version_start_including VARCHAR(255),
    version_start_excluding VARCHAR(255),
    version_end_including VARCHAR(255),
    version_end_excluding VARCHAR(255),
    published_date TIMESTAMP,
    last_modified TIMESTAMP,
    raw_json TEXT,
    synced_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_cve_id ON local_cve(cve_id);
CREATE INDEX idx_cpe_product ON local_cve(cpe_product);
CREATE INDEX idx_last_modified ON local_cve(last_modified);
