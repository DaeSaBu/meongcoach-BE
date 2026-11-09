package com.daesabu.meongcoach.community.adapter.webapi.dto;

import com.daesabu.meongcoach.community.domain.Comment;
import com.daesabu.meongcoach.community.domain.CommentMedia;
import com.daesabu.meongcoach.community.domain.PostMedia;
import com.daesabu.meongcoach.community.domain.PostMediaType;
import java.util.List;

public record PostMediaResponse(PostMediaType type, String url) {

	public static PostMediaResponse from(PostMedia media) {
		return new PostMediaResponse(media.getType(), media.getUrl());
	}

	public static PostMediaResponse from(CommentMedia media) {
		return new PostMediaResponse(media.getType(), media.getUrl());
	}

	public static List<PostMediaResponse> listOf(Comment comment) {
		return comment.getMedia()
				.stream()
				.map(PostMediaResponse::from)
				.toList();
	}
}
