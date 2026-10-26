package com.daesabu.meongcoach.community.adapter.webapi.dto;

import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;

public record PostMediaUploadUrlResponse(String uploadUrl, String publicUrl, long expiresInSeconds) {

	public static PostMediaUploadUrlResponse from(MediaUploadUrlResult result) {
		return new PostMediaUploadUrlResponse(result.uploadUrl(), result.publicUrl(), result.expiresInSeconds());
	}
}
