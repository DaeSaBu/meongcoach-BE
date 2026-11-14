package com.daesabu.meongcoach.support.adapter.webapi.dto;

import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;

public record ChatMediaUploadResponse(String uploadUrl, String objectKey, long expiresInSeconds) {
	public static ChatMediaUploadResponse from(MediaUploadUrlResult result) {
		return new ChatMediaUploadResponse(result.uploadUrl(), result.objectKey(), result.expiresInSeconds());
	}
}
