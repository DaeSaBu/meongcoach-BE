package com.daesabu.meongcoach.ai.application.provided;

import java.time.LocalDateTime;

/**
 * AI 리포트 상세 조회 결과. 본문을 구조화된 {@link AiReportContent}로 담는다.
 * title은 제목 생성에 실패한 리포트에서 null일 수 있다.
 */
public record AiReportDetailResult(Long id, String videoObjectKey, String title, AiReportContent content,
		LocalDateTime createdAt) {
}
