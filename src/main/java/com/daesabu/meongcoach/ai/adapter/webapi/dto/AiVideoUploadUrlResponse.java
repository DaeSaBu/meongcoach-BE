package com.daesabu.meongcoach.ai.adapter.webapi.dto;

import com.daesabu.meongcoach.ai.application.provided.AiVideoUploadUrlView;

public record AiVideoUploadUrlResponse(String uploadUrl, String publicUrl, String objectKey,
                                       long expiresInSeconds) {

	public static AiVideoUploadUrlResponse from(AiVideoUploadUrlView view) {
		return new AiVideoUploadUrlResponse(view.uploadUrl(), view.publicUrl(), view.objectKey(),
				view.expiresInSeconds());
	}
}
