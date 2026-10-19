package com.daesabu.meongcoach.community.adapter.webapi.dto;

import com.daesabu.meongcoach.community.application.provided.dto.PostListResult;
import java.util.List;

public record PostListResponse(List<PostResponse> posts, Long nextCursor) {

	public static PostListResponse from(PostListResult result) {
		List<PostResponse> posts = result.posts().stream()
				.map(PostResponse::from)
				.toList();
		return new PostListResponse(posts, result.nextCursor());
	}
}
