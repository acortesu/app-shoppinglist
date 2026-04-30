CREATE TABLE ingredient_custom (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    measurement_type VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_ingredient_custom_normalized_name UNIQUE (normalized_name)
);

CREATE INDEX idx_recipes_created_at ON recipes (created_at);
CREATE INDEX idx_meal_plans_created_at ON meal_plans (created_at);
CREATE INDEX idx_shopping_list_drafts_created_at ON shopping_list_drafts (created_at);
CREATE INDEX idx_shopping_list_drafts_plan_id ON shopping_list_drafts (plan_id);
CREATE INDEX idx_meal_plan_slots_recipe_id ON meal_plan_slots (recipe_id);
CREATE INDEX idx_shopping_list_draft_items_ingredient_id ON shopping_list_draft_items (ingredient_id);

ALTER TABLE recipes
    ADD CONSTRAINT chk_recipes_usage_count_non_negative CHECK (usage_count >= 0);

ALTER TABLE recipe_ingredients
    ADD CONSTRAINT chk_recipe_ingredients_quantity_positive CHECK (quantity > 0);

ALTER TABLE shopping_list_draft_items
    ADD CONSTRAINT chk_shopping_list_draft_items_quantity_positive CHECK (quantity > 0);

ALTER TABLE shopping_list_draft_items
    ADD CONSTRAINT chk_shopping_list_draft_items_package_amount_positive CHECK (package_amount IS NULL OR package_amount > 0);

ALTER TABLE shopping_list_draft_items
    ADD CONSTRAINT uq_shopping_list_draft_items_item_id_per_draft UNIQUE (draft_id, item_id);
