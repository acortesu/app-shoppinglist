ALTER TABLE recipes ADD COLUMN user_id VARCHAR(128);
UPDATE recipes SET user_id = 'legacy' WHERE user_id IS NULL;
ALTER TABLE recipes ALTER COLUMN user_id SET NOT NULL;
CREATE INDEX idx_recipes_user_id ON recipes (user_id);

ALTER TABLE meal_plans ADD COLUMN user_id VARCHAR(128);
UPDATE meal_plans SET user_id = 'legacy' WHERE user_id IS NULL;
ALTER TABLE meal_plans ALTER COLUMN user_id SET NOT NULL;
CREATE INDEX idx_meal_plans_user_id ON meal_plans (user_id);

ALTER TABLE shopping_list_drafts ADD COLUMN user_id VARCHAR(128);
UPDATE shopping_list_drafts SET user_id = 'legacy' WHERE user_id IS NULL;
ALTER TABLE shopping_list_drafts ALTER COLUMN user_id SET NOT NULL;
CREATE INDEX idx_shopping_list_drafts_user_id ON shopping_list_drafts (user_id);

ALTER TABLE ingredient_custom ADD COLUMN user_id VARCHAR(128);
UPDATE ingredient_custom SET user_id = 'legacy' WHERE user_id IS NULL;
ALTER TABLE ingredient_custom ALTER COLUMN user_id SET NOT NULL;
CREATE INDEX idx_ingredient_custom_user_id ON ingredient_custom (user_id);

ALTER TABLE ingredient_custom DROP CONSTRAINT IF EXISTS uq_ingredient_custom_normalized_name;
ALTER TABLE ingredient_custom ADD CONSTRAINT uq_ingredient_custom_user_normalized_name UNIQUE (user_id, normalized_name);
