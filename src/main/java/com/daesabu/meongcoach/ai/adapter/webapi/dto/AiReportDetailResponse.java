package com.daesabu.meongcoach.ai.adapter.webapi.dto;

import com.daesabu.meongcoach.ai.application.provided.AiReportContent;
import com.daesabu.meongcoach.ai.application.provided.AiReportDetailResult;
import com.daesabu.meongcoach.ai.domain.AiAnalysisStatus;
import java.time.LocalDateTime;

public record AiReportDetailResponse(Long reportId, String videoObjectKey, String title, AiAnalysisStatus status,
		AiReportContent content, LocalDateTime createdAt) {

	public static AiReportDetailResponse from(AiReportDetailResult result) {
		return new AiReportDetailResponse(result.id(), result.videoObjectKey(), result.title(), result.status(),
				result.content(), result.createdAt());
	}
}
