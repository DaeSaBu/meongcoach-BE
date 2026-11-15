package com.daesabu.meongcoach.analysis.application.provided;

public record AiAnalysisRegisterResult(Long analysisId, String uploadUrl, String objectKey, long expiresInSeconds) {
}
