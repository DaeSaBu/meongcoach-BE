package com.daesabu.meongcoach.media.adapter.webapi.dto;

import com.daesabu.meongcoach.media.application.provided.ImageUploadUrlResult;

public record ImageUploadUrlResponse(String uploadUrl, String publicUrl, long expiresInSeconds) {

	public static ImageUploadUrlResponse from(ImageUploadUrlResult result) {
		return new ImageUploadUrlResponse(result.uploadUrl(), result.publicUrl(), result.expiresInSeconds());
	}
}
