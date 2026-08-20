package com.daesabu.meongcoach.ai.adapter.webapi.dto;

import com.daesabu.meongcoach.ai.application.provided.AiReportResult;
import java.time.LocalDateTime;

/**
 * AI 리포트 목록 항목 응답. 본문은 상세 조회에서만 내린다. title은 제목 생성에 실패한 리포트에서 null일 수 있다.
 */
public record AiReportResponse(Long reportId, String videoObjectKey, String title, LocalDateTime createdAt) {

	public static AiReportResponse from(AiReportResult result) {
		return new AiReportResponse(result.id(), result.videoObjectKey(), result.title(), result.createdAt());
	}
}
