DROP INDEX IF EXISTS idx_shopping_list_drafts_idempotency_key;

ALTER TABLE shopping_list_drafts
    ADD CONSTRAINT uk_shopping_list_drafts_user_plan_idempotency_key
        UNIQUE (user_id, plan_id, idempotency_key);
