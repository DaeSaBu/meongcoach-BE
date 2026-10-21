package com.daesabu.meongcoach.community.adapter.webapi.dto;

import com.daesabu.meongcoach.user.application.provided.UserProfileSummaryResult;

public record AuthorResponse(Long userId, String nickname, String profileImageUrl) {

	public static AuthorResponse from(UserProfileSummaryResult result) {
		return new AuthorResponse(result.userId(), result.nickname(), result.profileImageUrl());
	}
}
