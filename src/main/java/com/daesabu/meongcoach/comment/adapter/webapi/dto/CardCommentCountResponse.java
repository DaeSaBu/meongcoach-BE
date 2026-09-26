package com.daesabu.meongcoach.comment.adapter.webapi.dto;

import com.daesabu.meongcoach.comment.application.provided.CardCommentCountResult;

public record CardCommentCountResponse(Long cardId, long totalCount) {

	public static CardCommentCountResponse from(CardCommentCountResult result) {
		return new CardCommentCountResponse(result.cardId(), result.totalCount());
	}
}
