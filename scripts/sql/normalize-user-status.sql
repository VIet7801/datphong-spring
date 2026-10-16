-- Run before starting the application against an existing PostgreSQL database.
-- Transactional and repeatable: unknown statuses make the new constraint fail,
-- rolling back the entire migration instead of silently activating accounts.
BEGIN;

ALTER TABLE public.users DROP CONSTRAINT IF EXISTS users_status_check;

UPDATE public.users
SET status = CASE status
    WHEN 'HOAT_DONG' THEN 'ACTIVE'
    WHEN 'KHONG_HOAT_DONG' THEN 'IN_ACTIVE'
    WHEN 'DISABLED' THEN 'IN_ACTIVE'
    WHEN 'PENDING_VERIFICATION' THEN 'IN_ACTIVE'
    ELSE status
END
WHERE status IN ('HOAT_DONG', 'KHONG_HOAT_DONG', 'DISABLED', 'PENDING_VERIFICATION');

ALTER TABLE public.users ADD CONSTRAINT users_status_check
    CHECK (status IN ('ACTIVE', 'IN_ACTIVE', 'LOCKED'));

-- Raw SQL inserts without an explicit status must not use the legacy default
-- PENDING_VERIFICATION (or silently activate an account).
ALTER TABLE public.users ALTER COLUMN status SET DEFAULT 'IN_ACTIVE';

COMMIT;
