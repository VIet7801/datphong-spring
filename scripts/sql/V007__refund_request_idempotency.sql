-- Runtime RefundService uses this key across retries and concurrent callers.
-- NULL keys from historical data are left intact and require reconciliation.
CREATE UNIQUE INDEX IF NOT EXISTS uq_refund_requests_idempotency_key
    ON refund_requests (idempotency_key) WHERE idempotency_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_refund_requests_due
    ON refund_requests (status, next_retry_at, id);
