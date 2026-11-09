package com.daesabu.meongcoach.dog.adapter.webapi.dto;

import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;

public record DogProfileImageUploadUrlResponse(String uploadUrl, String publicUrl, long expiresInSeconds) {

	public static DogProfileImageUploadUrlResponse from(MediaUploadUrlResult result) {
		return new DogProfileImageUploadUrlResponse(result.uploadUrl(), result.publicUrl(), result.expiresInSeconds());
	}
}
