-- Deduplicate local_cve and prevent recurrence.
-- Early sync runs (restarts before resume existed) inserted the same
-- (cve, vendor, product, version-range) identity more than once. Rows that
-- differ only in id/synced_at are content duplicates: keep the oldest.
-- Same vendor/product with DIFFERENT version ranges are legitimately
-- distinct rows (version matching needs them) and are preserved.
DELETE FROM local_cve
WHERE ctid NOT IN (
    SELECT min(ctid)
    FROM local_cve
    GROUP BY cve_id, cpe_vendor, cpe_product, description, cvss_score,
             cvss_version, severity, vector_string,
             version_start_including, version_start_excluding,
             version_end_including, version_end_excluding,
             published_date, last_modified
);

-- Backstop: the sync skips known cve_ids, but this guarantees no exact
-- identity can ever be inserted twice (NULLS NOT DISTINCT because version
-- bounds are often NULL, and plain UNIQUE would not compare them).
CREATE UNIQUE INDEX uq_local_cve_identity ON local_cve (
    cve_id, cpe_vendor, cpe_product,
    version_start_including, version_start_excluding,
    version_end_including, version_end_excluding
) NULLS NOT DISTINCT;
