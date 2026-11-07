package com.daesabu.meongcoach.ai.adapter.webapi.dto;

import com.daesabu.meongcoach.ai.application.provided.AiReportResult;
import com.daesabu.meongcoach.ai.domain.AiAnalysisStatus;
import java.time.LocalDateTime;

public record AiReportResponse(Long reportId, String videoObjectKey, String title, AiAnalysisStatus status,
		LocalDateTime createdAt) {

	public static AiReportResponse from(AiReportResult result) {
		return new AiReportResponse(result.id(), result.videoObjectKey(), result.title(), result.status(),
				result.createdAt());
	}
}
