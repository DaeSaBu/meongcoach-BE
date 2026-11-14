package com.daesabu.meongcoach.analysis.adapter.webapi.dto;

import com.daesabu.meongcoach.analysis.application.provided.AiAnalysisRegisterResult;

public record AiAnalysisRegisterResponse(Long analysisId, String uploadUrl, String objectKey, long expiresInSeconds) {

	public static AiAnalysisRegisterResponse from(AiAnalysisRegisterResult result) {
		return new AiAnalysisRegisterResponse(result.analysisId(), result.uploadUrl(), result.objectKey(),
				result.expiresInSeconds());
	}
}
