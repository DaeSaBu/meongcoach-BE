ALTER TABLE card_comment_likes ADD COLUMN card_id BIGINT;

UPDATE card_comment_likes AS likes
SET card_id = comments.card_id
FROM card_comments AS comments
WHERE likes.comment_id = comments.id;

ALTER TABLE card_comment_likes ALTER COLUMN card_id SET NOT NULL;

CREATE INDEX idx_card_comment_likes_card_id_comment_id
    ON card_comment_likes (card_id, comment_id);
