package com.daesabu.meongcoach.training_comment.adapter.webapi.dto;

import com.daesabu.meongcoach.training_comment.application.provided.CardCommentCountResult;

public record CardCommentCountResponse(Long cardId, long totalCount) {

	public static CardCommentCountResponse from(CardCommentCountResult result) {
		return new CardCommentCountResponse(result.cardId(), result.totalCount());
	}
}
