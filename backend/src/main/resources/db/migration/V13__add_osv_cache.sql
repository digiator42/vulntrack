-- OSV advisory cache and per-dependency match cache.
--
-- VulnTrack matches dependencies against two independent sources: the OSV
-- database (ecosystem-native, exact version) and the local NVD CPE cache
-- (CPE-based, broad). OSV has no documented rate limit, but re-fetching
-- advisory detail for every scan is wasteful and slow, so records are
-- cached here; dependency_vulnerability_cache additionally lets the future
-- re-verification job re-check a package version without re-querying.
CREATE TABLE osv_advisory (
    id VARCHAR(64) PRIMARY KEY,          -- GHSA-xxxx / PYSEC-xxxx / OSV-xxxx
    aliases TEXT,                         -- CVE ids and other alias ids
    summary TEXT,
    details TEXT,
    severity VARCHAR(32),
    cvss_vector TEXT,
    cvss_score DOUBLE PRECISION,
    cwe_ids TEXT,
    affected_refs TEXT,                   -- JSON array of affected package refs
    published_date TIMESTAMP,
    modified_date TIMESTAMP,
    raw_json TEXT,
    synced_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_osv_advisory_synced ON osv_advisory(synced_at);

-- Cached (dependency, version) -> advisory ids verdict, so repeat scans of an
-- unchanged manifest cost one table read per dependency.
CREATE TABLE dependency_vulnerability_cache (
    id BIGSERIAL PRIMARY KEY,
    ecosystem VARCHAR(32) NOT NULL,
    package_name VARCHAR(512) NOT NULL,
    package_version VARCHAR(128),
    advisory_ids TEXT,                    -- JSON array of OSV ids
    resolved_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (ecosystem, package_name, package_version)
);

CREATE INDEX idx_dvc_lookup
    ON dependency_vulnerability_cache(ecosystem, package_name, package_version);