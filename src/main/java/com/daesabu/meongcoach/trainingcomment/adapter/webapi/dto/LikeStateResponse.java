package com.daesabu.meongcoach.trainingcomment.adapter.webapi.dto;

import com.daesabu.meongcoach.trainingcomment.application.provided.LikeStateResult;

public record LikeStateResponse(long likeCount, boolean likedByMe) {

	public static LikeStateResponse from(LikeStateResult result) {
		return new LikeStateResponse(result.likeCount(), result.likedByMe());
	}
}
