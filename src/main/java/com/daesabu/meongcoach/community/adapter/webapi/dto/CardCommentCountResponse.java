package com.daesabu.meongcoach.community.adapter.webapi.dto;

import com.daesabu.meongcoach.community.application.provided.CommentCountResult;

public record CardCommentCountResponse(Long cardId, long totalCount) {

	public static CardCommentCountResponse from(CommentCountResult result) {
		return new CardCommentCountResponse(result.targetId(), result.totalCount());
	}
}
