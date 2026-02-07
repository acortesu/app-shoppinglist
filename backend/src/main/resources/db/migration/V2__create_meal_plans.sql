CREATE TABLE meal_plans (
    id VARCHAR(36) PRIMARY KEY,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    period VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE meal_plan_slots (
    plan_id VARCHAR(36) NOT NULL,
    position INTEGER NOT NULL,
    slot_date DATE NOT NULL,
    meal_type VARCHAR(32) NOT NULL,
    recipe_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (plan_id, position),
    CONSTRAINT fk_meal_plan_slots_plan
        FOREIGN KEY (plan_id)
        REFERENCES meal_plans(id)
        ON DELETE CASCADE
);
