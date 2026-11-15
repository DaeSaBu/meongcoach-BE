package com.daesabu.meongcoach.analysis.adapter.webapi.dto;

import com.daesabu.meongcoach.analysis.application.provided.AiReportContent;
import com.daesabu.meongcoach.analysis.application.provided.AiReportDetailResult;
import com.daesabu.meongcoach.analysis.domain.AiAnalysisStatus;
import java.time.LocalDateTime;

public record AiReportDetailResponse(Long reportId, String videoObjectKey, String videoUrl, String title,
		AiAnalysisStatus status, AiReportContent content, LocalDateTime createdAt) {

	public static AiReportDetailResponse from(AiReportDetailResult result) {
		return new AiReportDetailResponse(result.id(), result.videoObjectKey(), result.videoUrl(), result.title(),
				result.status(), result.content(), result.createdAt());
	}
}
