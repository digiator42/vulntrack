-- NVD sync progress tracking.
-- A full NVD sync pages ~250k CVEs over ~30 minutes on a background
-- thread. If the container restarts (deploy, recycle) mid-sync, the run
-- dies silently with no progress saved. This single-row table records the
-- next page to fetch plus the run state, so a new trigger (or boot
-- recovery) resumes instead of starting over, and the admin status
-- endpoint can report a real position.
CREATE TABLE nvd_sync_state (
    id INTEGER PRIMARY KEY,
    last_start_index INTEGER NOT NULL DEFAULT 0,
    total_results INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'IDLE',
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO nvd_sync_state (id, last_start_index, status) VALUES (1, 0, 'IDLE');
