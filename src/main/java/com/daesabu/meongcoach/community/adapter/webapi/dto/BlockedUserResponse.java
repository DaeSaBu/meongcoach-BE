package com.daesabu.meongcoach.community.adapter.webapi.dto;

import com.daesabu.meongcoach.community.application.provided.dto.BlockedUserResult;

public record BlockedUserResponse(Long userId, String nickname, String profileImageUrl) {

	public static BlockedUserResponse from(BlockedUserResult result) {
		if (result.author() == null) {
			return new BlockedUserResponse(result.userId(), null, null);
		}
		AuthorResponse author = AuthorResponse.from(result.author());
		return new BlockedUserResponse(result.userId(), author.nickname(), author.profileImageUrl());
	}
}
