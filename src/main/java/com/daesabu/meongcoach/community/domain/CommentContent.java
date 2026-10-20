package com.daesabu.meongcoach.community.domain;

import com.daesabu.meongcoach.community.domain.exception.InvalidCommentContentException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record CommentContent(@Column(name = "content", nullable = false, columnDefinition = "TEXT") String value) {

	public static final int MAX_LENGTH = 500;

	public CommentContent {
		if (value == null || value.isBlank()) {
			throw new InvalidCommentContentException("본문이 비어 있습니다.");
		}
		if (value.length() > MAX_LENGTH) {
			throw new InvalidCommentContentException("본문은 " + MAX_LENGTH + "자를 넘을 수 없습니다.");
		}
	}
}
