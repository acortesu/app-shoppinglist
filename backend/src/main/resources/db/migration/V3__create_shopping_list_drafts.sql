CREATE TABLE shopping_list_drafts (
    id VARCHAR(36) PRIMARY KEY,
    plan_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE shopping_list_draft_items (
    draft_id VARCHAR(36) NOT NULL,
    position INTEGER NOT NULL,
    item_id VARCHAR(64) NOT NULL,
    ingredient_id VARCHAR(128),
    name VARCHAR(255) NOT NULL,
    quantity DOUBLE PRECISION NOT NULL,
    unit VARCHAR(64) NOT NULL,
    suggested_packages INTEGER,
    package_amount DOUBLE PRECISION,
    package_unit VARCHAR(64),
    manual BOOLEAN NOT NULL,
    PRIMARY KEY (draft_id, position),
    CONSTRAINT fk_shopping_list_draft_items_draft
        FOREIGN KEY (draft_id)
        REFERENCES shopping_list_drafts(id)
        ON DELETE CASCADE
);
