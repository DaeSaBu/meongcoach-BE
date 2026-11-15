package com.daesabu.meongcoach.community.adapter.webapi.dto;

import com.daesabu.meongcoach.community.domain.LikeSummary;

public record LikeResponse(long likeCount, boolean likedByMe) {
	public static LikeResponse from(LikeSummary result) {
		return new LikeResponse(result.likeCount(), result.likedByMe());
	}
}
