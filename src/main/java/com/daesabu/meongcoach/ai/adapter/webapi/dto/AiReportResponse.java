package com.daesabu.meongcoach.ai.adapter.webapi.dto;

import com.daesabu.meongcoach.ai.application.provided.AiReportResult;
import com.daesabu.meongcoach.ai.domain.AiReportStatus;
import java.time.LocalDateTime;

/**
 * AI 리포트 목록 항목 응답. 본문은 상세 조회에서만 내린다.
 * status가 COMPLETED가 아니면 title은 null이며, COMPLETED여도 제목 생성에 실패한 리포트는 null일 수 있다.
 */
public record AiReportResponse(Long reportId, String videoObjectKey, String title, AiReportStatus status,
		LocalDateTime createdAt) {

	public static AiReportResponse from(AiReportResult result) {
		return new AiReportResponse(result.id(), result.videoObjectKey(), result.title(), result.status(),
				result.createdAt());
	}
}
