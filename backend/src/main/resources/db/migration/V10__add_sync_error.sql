-- Observability for NVD sync failures.
-- When a sync dies (NVD throttle, network, container recycle), the only
-- trace was a backend log line. Persisting the last failure reason lets
-- the admin status endpoint (and UI badge) show WHY a run stopped.
ALTER TABLE nvd_sync_state ADD COLUMN last_error TEXT;
