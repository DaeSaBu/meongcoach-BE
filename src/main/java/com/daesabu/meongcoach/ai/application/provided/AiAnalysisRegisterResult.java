package com.daesabu.meongcoach.ai.application.provided;

public record AiAnalysisRegisterResult(Long analysisId, String uploadUrl, String objectKey, long expiresInSeconds) {
}
