package com.daesabu.meongcoach.community.adapter.webapi.dto;

import com.daesabu.meongcoach.community.application.provided.dto.AuthorResult;

public record AuthorResponse(Long userId, String nickname, String profileImageUrl) {

	private static final String GUARDIAN_SUFFIX = " 보호자";

	public static AuthorResponse from(AuthorResult result) {
		return new AuthorResponse(result.userId(), result.dogName() + GUARDIAN_SUFFIX, result.profileImageUrl());
	}
}
