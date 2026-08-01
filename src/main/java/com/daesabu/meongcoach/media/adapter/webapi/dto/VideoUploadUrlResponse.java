package com.daesabu.meongcoach.media.adapter.webapi.dto;

import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlResult;

public record VideoUploadUrlResponse(String uploadUrl, String publicUrl, String objectKey, long expiresInSeconds) {

	public static VideoUploadUrlResponse from(VideoUploadUrlResult result) {
		return new VideoUploadUrlResponse(result.uploadUrl(), result.publicUrl(), result.objectKey(),
				result.expiresInSeconds());
	}
}
