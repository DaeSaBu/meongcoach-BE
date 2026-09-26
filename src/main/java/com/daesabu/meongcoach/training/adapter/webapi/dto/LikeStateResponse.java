package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.LikeStateResult;

public record LikeStateResponse(long likeCount, boolean likedByMe) {

	public static LikeStateResponse from(LikeStateResult result) {
		return new LikeStateResponse(result.likeCount(), result.likedByMe());
	}
}
