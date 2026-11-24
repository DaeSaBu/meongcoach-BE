package com.daesabu.meongcoach.community.adapter.webapi.dto;

import com.daesabu.meongcoach.community.application.provided.dto.BlockedUserResult;
import java.util.List;

public record BlockListResponse(List<BlockedUserResponse> blocks) {

	public static BlockListResponse from(List<BlockedUserResult> results) {
		List<BlockedUserResponse> blocks = results.stream()
				.map(BlockedUserResponse::from)
				.toList();
		return new BlockListResponse(blocks);
	}
}
