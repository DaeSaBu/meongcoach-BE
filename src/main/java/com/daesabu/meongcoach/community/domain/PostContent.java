package com.daesabu.meongcoach.community.domain;

import com.daesabu.meongcoach.community.domain.exception.InvalidPostContentException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record PostContent(@Column(name = "content", nullable = false, columnDefinition = "TEXT") String value) {

	public static final int MAX_LENGTH = 200;

	public PostContent {
		if (value == null || value.isBlank()) {
			throw new InvalidPostContentException("본문이 비어 있습니다.");
		}
		if (value.length() > MAX_LENGTH) {
			throw new InvalidPostContentException("본문은 " + MAX_LENGTH + "자를 넘을 수 없습니다.");
		}
	}
}
