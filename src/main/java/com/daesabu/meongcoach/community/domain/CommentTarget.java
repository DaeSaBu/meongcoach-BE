package com.daesabu.meongcoach.community.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * 댓글이 붙는 대상. 다른 모듈의 엔티티를 종류와 ID 값으로만 가리키며 연관도 FK도 두지 않는다.
 */
@Embeddable
public record CommentTarget(
		@Enumerated(EnumType.STRING) @Column(name = "target_type", nullable = false, length = 20) CommentTargetType type,
		@Column(name = "target_id", nullable = false) Long id) {

	public static CommentTarget card(Long cardId) {
		return new CommentTarget(CommentTargetType.CARD, cardId);
	}
}
