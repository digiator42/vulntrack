-- Allow NULL tokens: POST /api/auth/logout clears the stored token on
-- logout (user.setToken(null)), but V2 declared the column NOT NULL, so
-- every logout fails with a constraint violation. The token column is
-- informational only (JWTs are validated cryptographically), so NULL
-- simply means "logged out / no active token".
ALTER TABLE users
    ALTER COLUMN token DROP NOT NULL;
