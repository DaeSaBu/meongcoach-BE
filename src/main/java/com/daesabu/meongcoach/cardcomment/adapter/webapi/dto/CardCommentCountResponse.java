package com.daesabu.meongcoach.cardcomment.adapter.webapi.dto;

import com.daesabu.meongcoach.cardcomment.application.provided.CardCommentCountResult;

public record CardCommentCountResponse(Long cardId, long totalCount) {

	public static CardCommentCountResponse from(CardCommentCountResult result) {
		return new CardCommentCountResponse(result.cardId(), result.totalCount());
	}
}
