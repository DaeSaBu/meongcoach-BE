package com.daesabu.meongcoach.community.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PostMentionTest {

	@Test
	void 멘션하면_대상이_채워진다() {
		PostMention mention = PostMention.create(new MentionCommand(MentionTargetType.LESSON, 10L));

		assertThat(mention.getTargetType()).isEqualTo(MentionTargetType.LESSON);
		assertThat(mention.getTargetId()).isEqualTo(10L);
	}
}
