ALTER TABLE plans
    ADD COLUMN completed_lesson_ids BIGINT[] NOT NULL DEFAULT '{}';
