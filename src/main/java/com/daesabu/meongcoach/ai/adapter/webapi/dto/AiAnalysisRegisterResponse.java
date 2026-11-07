package com.daesabu.meongcoach.ai.adapter.webapi.dto;

import com.daesabu.meongcoach.ai.application.provided.AiAnalysisRegisterResult;

public record AiAnalysisRegisterResponse(Long analysisId, String uploadUrl, String objectKey, long expiresInSeconds) {

	public static AiAnalysisRegisterResponse from(AiAnalysisRegisterResult result) {
		return new AiAnalysisRegisterResponse(result.analysisId(), result.uploadUrl(), result.objectKey(),
				result.expiresInSeconds());
	}
}
