-- Drop the write-only raw_json payload on local_cve.
-- It stored a full NVD CVE document per (cve, vendor, product) row, was
-- never read by any query or API, and single-handedly filled the 1 GB
-- Neon project quota mid-sync ("project size limit (1024 MB) has been
-- exceeded"). Rows keep every column the scanners actually use.
ALTER TABLE local_cve DROP COLUMN raw_json;
