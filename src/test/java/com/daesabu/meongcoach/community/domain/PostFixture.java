package com.daesabu.meongcoach.community.domain;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

public final class PostFixture {

	private PostFixture() {
	}

	public static Post create(Long id, Long userId, String content, LocalDateTime createdAt, List<Long> cardIds) {
		List<MentionCommand> mentions = cardIds.stream()
				.map(cardId -> new MentionCommand(MentionTargetType.CARD, cardId))
				.toList();
		Post post = Post.write(new PostWriteCommand(userId, content, mentions));
		ReflectionTestUtils.setField(post, "id", id);
		ReflectionTestUtils.setField(post, "createdAt", createdAt);
		return post;
	}
}
