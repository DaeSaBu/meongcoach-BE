package com.daesabu.meongcoach.training_comment.application.provided.dto;

import com.daesabu.meongcoach.training_comment.domain.CardCommentCreateCommand;

// 본문 검증은 CardComment가 도메인 규칙으로 수행하므로 제약 어노테이션을 두지 않는다
public record CommentCreateRequest(String content) {

	public CardCommentCreateCommand toCommand(Long cardId, Long authorId) {
		return new CardCommentCreateCommand(cardId, authorId, content);
	}
}
