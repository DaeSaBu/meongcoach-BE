DROP INDEX IF EXISTS uk_plans_active_user;

UPDATE plans AS candidate
SET status = 'ACTIVE'
WHERE candidate.status = 'REPLACED'
  AND candidate.id IN (
      SELECT DISTINCT ON (user_id, dog_id) id
      FROM plans
      ORDER BY user_id, dog_id, id DESC
  )
  AND EXISTS (
      SELECT 1 FROM dogs AS dog
      WHERE dog.id = candidate.dog_id
        AND dog.user_id = candidate.user_id
        AND dog.deleted_at IS NULL
  )
  AND NOT EXISTS (
      SELECT 1 FROM plans AS active
      WHERE active.user_id = candidate.user_id
        AND active.dog_id = candidate.dog_id
        AND active.status = 'ACTIVE'
  );

CREATE UNIQUE INDEX IF NOT EXISTS uk_plans_active_user_dog
    ON plans (user_id, dog_id) WHERE status = 'ACTIVE';
