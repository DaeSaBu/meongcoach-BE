package com.daesabu.meongcoach.ai.adapter.webapi.dto;

import com.daesabu.meongcoach.ai.application.provided.AiReportVideoUploadUrlView;

public record AiReportVideoUploadUrlResponse(String uploadUrl, String publicUrl, String objectKey,
                                             long expiresInSeconds) {

	public static AiReportVideoUploadUrlResponse from(AiReportVideoUploadUrlView view) {
		return new AiReportVideoUploadUrlResponse(view.uploadUrl(), view.publicUrl(), view.objectKey(),
				view.expiresInSeconds());
	}
}
