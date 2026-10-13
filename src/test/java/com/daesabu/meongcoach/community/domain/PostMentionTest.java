package com.daesabu.meongcoach.community.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PostMentionTest {

	@Test
	void 멘션하면_대상이_채워진다() {
		PostMention mention = PostMention.mention(new MentionCommand(MentionTargetType.CARD, 10L));

		assertThat(mention.getTargetType()).isEqualTo(MentionTargetType.CARD);
		assertThat(mention.getTargetId()).isEqualTo(10L);
	}

	@Test
	void 멘션_대상_종류를_판단한다() {
		PostMention mention = PostMention.mention(new MentionCommand(MentionTargetType.CARD, 10L));

		assertThat(mention.isTarget(MentionTargetType.CARD)).isTrue();
	}
}
