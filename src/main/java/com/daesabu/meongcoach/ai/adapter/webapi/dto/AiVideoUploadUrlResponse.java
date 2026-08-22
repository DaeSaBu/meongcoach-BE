package com.daesabu.meongcoach.ai.adapter.webapi.dto;

import com.daesabu.meongcoach.ai.application.provided.AiVideoUploadUrlResult;

public record AiVideoUploadUrlResponse(Long reportId, String uploadUrl, String publicUrl, String objectKey,
                                       long expiresInSeconds) {

	public static AiVideoUploadUrlResponse from(AiVideoUploadUrlResult result) {
		return new AiVideoUploadUrlResponse(result.reportId(), result.uploadUrl(), result.publicUrl(),
				result.objectKey(), result.expiresInSeconds());
	}
}
