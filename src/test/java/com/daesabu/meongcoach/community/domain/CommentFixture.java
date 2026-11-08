package com.daesabu.meongcoach.community.domain;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

public final class CommentFixture {

	private CommentFixture() {
	}

	public static Comment create(Long id, Post post, Long userId, String content, LocalDateTime createdAt) {
		return create(id, post, userId, content, List.of(), createdAt);
	}

	public static Comment create(Long id, Post post, Long userId, String content, List<PostMediaCommand> media,
			LocalDateTime createdAt) {
		Comment comment = Comment.register(post, userId, content, media);
		ReflectionTestUtils.setField(comment, "id", id);
		ReflectionTestUtils.setField(comment, "createdAt", createdAt);
		return comment;
	}
}
