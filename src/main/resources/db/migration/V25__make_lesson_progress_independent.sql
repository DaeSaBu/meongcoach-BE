ALTER TABLE lesson_progress ADD COLUMN user_id BIGINT;

UPDATE lesson_progress lp
SET user_id = tp.user_id
FROM curriculum_progress cp
JOIN topic_progress tp ON tp.id = cp.topic_progress_id
WHERE cp.id = lp.curriculum_progress_id;

ALTER TABLE lesson_progress ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE lesson_progress ADD CONSTRAINT lesson_progress_user_lesson_unique UNIQUE (user_id, lesson_id);
ALTER TABLE lesson_progress DROP COLUMN curriculum_progress_id;

DROP TABLE curriculum_progress;
DROP TABLE topic_progress;
