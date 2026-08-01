BEGIN;

ALTER TABLE dogs
	ADD COLUMN IF NOT EXISTS expectation VARCHAR(500) NOT NULL DEFAULT '';

ALTER TABLE user_profiles
	ADD COLUMN IF NOT EXISTS walk_public BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE user_profiles
	ADD COLUMN IF NOT EXISTS match_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS user_prior_training_topics
(
	user_id  BIGINT NOT NULL,
	topic_id BIGINT NOT NULL,
	CONSTRAINT pk_user_prior_training_topics PRIMARY KEY (user_id, topic_id),
	CONSTRAINT fk_user_prior_training_topics_user_profile
		FOREIGN KEY (user_id) REFERENCES user_profiles (user_id) ON DELETE CASCADE,
	CONSTRAINT fk_user_prior_training_topics_topic
		FOREIGN KEY (topic_id) REFERENCES topics (id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_user_prior_training_topic_id
	ON user_prior_training_topics (topic_id);

CREATE TABLE IF NOT EXISTS user_training_goal_topics
(
	user_id  BIGINT NOT NULL,
	topic_id BIGINT NOT NULL,
	CONSTRAINT pk_user_training_goal_topics PRIMARY KEY (user_id, topic_id),
	CONSTRAINT fk_user_training_goal_topics_user_profile
		FOREIGN KEY (user_id) REFERENCES user_profiles (user_id) ON DELETE CASCADE,
	CONSTRAINT fk_user_training_goal_topics_topic
		FOREIGN KEY (topic_id) REFERENCES topics (id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_user_training_goal_topic_id
	ON user_training_goal_topics (topic_id);

COMMIT;
