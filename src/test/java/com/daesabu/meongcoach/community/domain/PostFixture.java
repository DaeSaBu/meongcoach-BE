package com.daesabu.meongcoach.community.domain;

import java.time.LocalDateTime;
import org.springframework.test.util.ReflectionTestUtils;

public final class PostFixture {

	private PostFixture() {
	}

	public static Post create(Long id, Long userId, String content, LocalDateTime createdAt) {
		return create(id, userId, content, null, createdAt);
	}

	public static Post create(Long id, Long userId, String content, MentionCommand mention, LocalDateTime createdAt) {
		Post post = Post.register(userId, new PostWriteCommand(content, mention));
		ReflectionTestUtils.setField(post, "id", id);
		ReflectionTestUtils.setField(post, "createdAt", createdAt);
		return post;
	}
}
