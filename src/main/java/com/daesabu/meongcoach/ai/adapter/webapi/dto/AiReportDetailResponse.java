package com.daesabu.meongcoach.ai.adapter.webapi.dto;

import com.daesabu.meongcoach.ai.application.provided.AiReportContent;
import com.daesabu.meongcoach.ai.application.provided.AiReportDetailResult;
import java.time.LocalDateTime;

/**
 * AI 리포트 상세 조회 응답. 본문을 구조화된 {@link AiReportContent}로 내려 프론트가 컴포넌트별로 스타일을 입힌다.
 * title은 제목 생성에 실패한 리포트에서 null일 수 있다.
 */
public record AiReportDetailResponse(Long reportId, String videoObjectKey, String title, AiReportContent content,
		LocalDateTime createdAt) {

	public static AiReportDetailResponse from(AiReportDetailResult result) {
		return new AiReportDetailResponse(result.id(), result.videoObjectKey(), result.title(), result.content(),
				result.createdAt());
	}
}
