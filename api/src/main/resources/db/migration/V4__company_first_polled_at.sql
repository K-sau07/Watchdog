-- When we started watching each board, so catch-time can mean something.
--
-- catch_time = first_seen_at - posted_at. On a board's FIRST poll that figure is
-- garbage: the ATS published those roles weeks or months before we existed, so every
-- backfilled row looks "caught" absurdly late. With 364 boards mostly backfilled, the
-- median today reads 88 days — arithmetically correct and completely useless as the
-- number the product puts in its largest type.
--
-- The fix is not to drop outliers, which would be inventing a cleaner story than the
-- data supports. It is to record the one fact that was missing: a posting was genuinely
-- *caught* only if it was posted after we began watching its board. Everything else is
-- backfill, and backfill has no catch-time at all.

ALTER TABLE company ADD COLUMN first_polled_at TIMESTAMPTZ;

-- Backfill: the earliest thing we ever saw from a board is when we started watching it.
-- This is an estimate and it is the best one available — it cannot be later than the
-- true start, so it never counts a backfilled posting as a catch.
UPDATE company c
SET first_polled_at = sub.started
FROM (
    SELECT company_id, MIN(first_seen_at) AS started
    FROM posting
    GROUP BY company_id
) AS sub
WHERE c.id = sub.company_id;

-- Boards registered but never polled keep NULL and contribute nothing until they are.
-- Boards polled before this column existed but holding no postings also stay NULL; a
-- NULL here means "unknown start", and unknown never counts as a catch.

CREATE INDEX idx_posting_catch_window ON posting (company_id, posted_at)
    WHERE posted_at IS NOT NULL;

COMMENT ON COLUMN company.first_polled_at IS
    'First time this board was polled. Backfilled from MIN(posting.first_seen_at). A '
    'posting only counts as caught when posted_at >= this; anything earlier is backfill '
    'that the ATS published before we were watching.';
