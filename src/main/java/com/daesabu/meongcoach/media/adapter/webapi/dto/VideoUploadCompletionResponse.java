package com.daesabu.meongcoach.media.adapter.webapi.dto;

import com.daesabu.meongcoach.media.application.provided.VerifiedVideoResult;

public record VideoUploadCompletionResponse(String objectKey, String publicUrl, String contentType, long sizeBytes) {

	public static VideoUploadCompletionResponse from(VerifiedVideoResult result) {
		return new VideoUploadCompletionResponse(result.objectKey(), result.publicUrl(), result.contentType(),
				result.sizeBytes());
	}
}
