package com.daesabu.meongcoach.analysis.adapter.webapi.dto;

import com.daesabu.meongcoach.analysis.application.provided.AiVideoUploadUrlResult;

public record AiVideoUploadUrlResponse(Long reportId, String uploadUrl, String publicUrl, String objectKey,
                                       long expiresInSeconds) {

	public static AiVideoUploadUrlResponse from(AiVideoUploadUrlResult result) {
		return new AiVideoUploadUrlResponse(result.reportId(), result.uploadUrl(), result.publicUrl(),
				result.objectKey(), result.expiresInSeconds());
	}
}
