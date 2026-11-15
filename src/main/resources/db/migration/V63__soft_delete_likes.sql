ALTER TABLE post_likes ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE comment_likes ADD COLUMN deleted_at TIMESTAMP;
