-- Comments Table
CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    author_id BIGINT NOT NULL REFERENCES users(id),
    issue_id BIGINT NOT NULL REFERENCES issues(id) ON DELETE CASCADE
);

CREATE INDEX idx_comments_issue ON comments(issue_id);

-- Activity Log Table (System Events)
CREATE TABLE activity_logs (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(50) NOT NULL, -- e.g., "STATUS_CHANGE", "ASSIGNED"
    old_value VARCHAR(255),
    new_value VARCHAR(255),
    performed_by BIGINT NOT NULL REFERENCES users(id),
    issue_id BIGINT NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_activity_issue ON activity_logs(issue_id);