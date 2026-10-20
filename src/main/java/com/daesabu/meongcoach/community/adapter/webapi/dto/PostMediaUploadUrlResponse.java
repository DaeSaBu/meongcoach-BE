package com.daesabu.meongcoach.community.adapter.webapi.dto;

import com.daesabu.meongcoach.media.application.provided.CommunityMediaUploadUrlResult;

public record PostMediaUploadUrlResponse(String uploadUrl, String publicUrl, long expiresInSeconds) {

	public static PostMediaUploadUrlResponse from(CommunityMediaUploadUrlResult result) {
		return new PostMediaUploadUrlResponse(result.uploadUrl(), result.publicUrl(), result.expiresInSeconds());
	}
}
