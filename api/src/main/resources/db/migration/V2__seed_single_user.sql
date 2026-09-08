-- Seed the single v1 user (D-WD5 single-user, no-auth; D-WD11 seed via migration).
-- Every per-user row (job_state, filter_profile) FKs to app_user, so the sole
-- user must exist before the workflow endpoints (S7) can write. The id is the
-- well-known constant SingleUser.ID in the application layer; keep them in sync.
-- Idempotent: safe to re-run, and harmless once real multi-user auth arrives.
INSERT INTO app_user (id, username)
VALUES ('00000000-0000-0000-0000-000000000001', 'watchdog')
ON CONFLICT (id) DO NOTHING;
