package com.daesabu.meongcoach.ai.adapter.webapi.dto;

import com.daesabu.meongcoach.ai.application.provided.AiReportDetailView;
import java.time.LocalDateTime;

/**
 * AI 리포트 상세 조회 응답. 리포트 본문을 포함한다.
 */
public record AiReportDetailResponse(Long reportId, String videoObjectKey, String content, LocalDateTime createdAt) {

	public static AiReportDetailResponse from(AiReportDetailView view) {
		return new AiReportDetailResponse(view.id(), view.videoObjectKey(), view.content(), view.createdAt());
	}
}
