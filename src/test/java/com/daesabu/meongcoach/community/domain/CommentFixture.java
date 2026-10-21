package com.daesabu.meongcoach.community.domain;

import java.time.LocalDateTime;
import org.springframework.test.util.ReflectionTestUtils;

public final class CommentFixture {

	private CommentFixture() {
	}

	public static Comment create(Long id, Post post, Long userId, String content, LocalDateTime createdAt) {
		Comment comment = Comment.register(post, userId, content);
		ReflectionTestUtils.setField(comment, "id", id);
		ReflectionTestUtils.setField(comment, "createdAt", createdAt);
		return comment;
	}
}
