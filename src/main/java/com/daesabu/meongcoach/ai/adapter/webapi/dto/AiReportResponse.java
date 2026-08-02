package com.daesabu.meongcoach.ai.adapter.webapi.dto;

import com.daesabu.meongcoach.ai.application.provided.AiReportView;
import java.time.LocalDateTime;

/**
 * AI 리포트 목록 항목 응답. 본문은 상세 조회에서만 내린다.
 */
public record AiReportResponse(Long reportId, String videoObjectKey, LocalDateTime createdAt) {

	public static AiReportResponse from(AiReportView view) {
		return new AiReportResponse(view.id(), view.videoObjectKey(), view.createdAt());
	}
}
