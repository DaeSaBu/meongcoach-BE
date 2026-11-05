DROP INDEX IF EXISTS uk_plans_active_user;

CREATE UNIQUE INDEX IF NOT EXISTS uk_plans_active_user_dog
    ON plans (user_id, dog_id) WHERE status = 'ACTIVE';
