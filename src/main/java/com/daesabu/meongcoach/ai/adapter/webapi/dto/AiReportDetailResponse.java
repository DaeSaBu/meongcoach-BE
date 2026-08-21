package com.daesabu.meongcoach.ai.adapter.webapi.dto;

import com.daesabu.meongcoach.ai.application.provided.AiReportContent;
import com.daesabu.meongcoach.ai.application.provided.AiReportDetailResult;
import com.daesabu.meongcoach.ai.domain.AiReportStatus;
import java.time.LocalDateTime;

/**
 * AI 리포트 상세 조회 응답. 본문을 구조화된 {@link AiReportContent}로 내려 프론트가 컴포넌트별로 스타일을 입힌다.
 * status가 COMPLETED가 아니면 title·content는 null이며, COMPLETED여도 제목 생성에 실패한 리포트는 title이 null일 수 있다.
 */
public record AiReportDetailResponse(Long reportId, String videoObjectKey, String title, AiReportStatus status,
		AiReportContent content, LocalDateTime createdAt) {

	public static AiReportDetailResponse from(AiReportDetailResult result) {
		return new AiReportDetailResponse(result.id(), result.videoObjectKey(), result.title(), result.status(),
				result.content(), result.createdAt());
	}
}
