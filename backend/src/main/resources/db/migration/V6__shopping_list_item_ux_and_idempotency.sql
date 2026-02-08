ALTER TABLE shopping_list_drafts
    ADD COLUMN idempotency_key VARCHAR(128);

CREATE INDEX idx_shopping_list_drafts_idempotency_key
    ON shopping_list_drafts (user_id, plan_id, idempotency_key);

ALTER TABLE shopping_list_draft_items
    ADD COLUMN bought BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE shopping_list_draft_items
    ADD COLUMN note TEXT;

ALTER TABLE shopping_list_draft_items
    ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0;

UPDATE shopping_list_draft_items
SET sort_order = position
WHERE sort_order = 0;
