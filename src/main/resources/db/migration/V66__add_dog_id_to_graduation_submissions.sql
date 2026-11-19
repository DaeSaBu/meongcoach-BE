ALTER TABLE graduation_submissions ADD COLUMN dog_id BIGINT;

UPDATE graduation_submissions s
SET dog_id = COALESCE(
    (SELECT MIN(d.id) FROM dogs d
     WHERE d.user_id = s.user_id AND d.status = 'SELECTED' AND d.deleted_at IS NULL),
    (SELECT MIN(d.id) FROM dogs d WHERE d.user_id = s.user_id));

DELETE FROM graduation_submissions WHERE dog_id IS NULL;

ALTER TABLE graduation_submissions ALTER COLUMN dog_id SET NOT NULL;

DROP INDEX uk_graduation_submissions_active;
DROP INDEX idx_graduation_submissions_user_topic_id;

CREATE UNIQUE INDEX uk_graduation_submissions_active
    ON graduation_submissions (user_id, dog_id, topic_id)
    WHERE status IN ('SUBMITTED', 'APPROVED');
CREATE INDEX idx_graduation_submissions_user_dog_topic_id
    ON graduation_submissions (user_id, dog_id, topic_id, id DESC);
