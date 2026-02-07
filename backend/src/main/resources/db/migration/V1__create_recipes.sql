CREATE TABLE recipes (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL,
    preparation TEXT,
    notes TEXT,
    usage_count INTEGER NOT NULL,
    last_used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE recipe_ingredients (
    recipe_id VARCHAR(36) NOT NULL,
    position INTEGER NOT NULL,
    ingredient_id VARCHAR(128) NOT NULL,
    quantity DOUBLE PRECISION NOT NULL,
    unit VARCHAR(32) NOT NULL,
    PRIMARY KEY (recipe_id, position),
    CONSTRAINT fk_recipe_ingredients_recipe
        FOREIGN KEY (recipe_id)
        REFERENCES recipes(id)
        ON DELETE CASCADE
);

CREATE TABLE recipe_tags (
    recipe_id VARCHAR(36) NOT NULL,
    tag VARCHAR(128) NOT NULL,
    PRIMARY KEY (recipe_id, tag),
    CONSTRAINT fk_recipe_tags_recipe
        FOREIGN KEY (recipe_id)
        REFERENCES recipes(id)
        ON DELETE CASCADE
);
