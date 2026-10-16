-- Guard against duplicate CVE cache rows.
--
-- NOTE (prod 2026-10-16): the one-time DELETE of legacy exact-dupe rows was
-- applied manually via the Neon editor, NOT here — a full-table cleanup plus
-- index build over ~1M rows exceeds Vercel's ~28 s container startup probe,
-- which put the backend into a boot loop. Fresh databases never contain
-- legacy dupes, so only the guard index ships in this migration (instant on
-- an empty table, skipped via IF NOT EXISTS where already applied).
--
-- Same vendor/product with DIFFERENT version ranges are legitimately
-- distinct rows (version matching needs them); only fully identical rows
-- (ignoring id/synced_at) were removed.
CREATE UNIQUE INDEX IF NOT EXISTS uq_local_cve_identity ON local_cve (
    cve_id, cpe_vendor, cpe_product,
    version_start_including, version_start_excluding,
    version_end_including, version_end_excluding
) NULLS NOT DISTINCT;
