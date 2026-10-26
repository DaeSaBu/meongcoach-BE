package com.daesabu.meongcoach.community.adapter.webapi.dto;

import com.daesabu.meongcoach.community.domain.PostMedia;
import com.daesabu.meongcoach.community.domain.PostMediaType;

public record PostMediaResponse(PostMediaType type, String url) {

	public static PostMediaResponse from(PostMedia media) {
		return new PostMediaResponse(media.getType(), media.getUrl());
	}
}
