package com.daesabu.meongcoach.ai.adapter.webapi.dto;

import com.daesabu.meongcoach.ai.application.provided.AiReportStatusResult;
import com.daesabu.meongcoach.ai.domain.AiReportStatus;

/**
 * AI 리포트 상태 폴링 응답. 식별자와 상태만 내리며 제목·본문은 상세 조회에서 받는다.
 */
public record AiReportStatusResponse(Long reportId, AiReportStatus status) {

	public static AiReportStatusResponse from(AiReportStatusResult result) {
		return new AiReportStatusResponse(result.id(), result.status());
	}
}
