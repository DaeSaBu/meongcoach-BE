package com.daesabu.meongcoach.trainingcomment.application.provided.dto;

import com.daesabu.meongcoach.trainingcomment.domain.CardCommentCreateCommand;
import jakarta.validation.constraints.NotBlank;

public record CommentCreateRequest(
		// 길이 위반은 전용 에러 코드로 내려야 해서 길이 검증은 CardComment가 맡는다
		@NotBlank String content) {

	public CardCommentCreateCommand toCommand(Long cardId, Long authorId) {
		return new CardCommentCreateCommand(cardId, authorId, content);
	}
}
