package com.daesabu.meongcoach.ai.application.provided;

import com.daesabu.meongcoach.ai.domain.AiReportStatus;
import java.time.LocalDateTime;

/**
 * AI 리포트 상세 조회 결과. 본문을 구조화된 {@link AiReportContent}로 담는다.
 * status가 COMPLETED가 아니면 title·content는 null이며, COMPLETED여도 제목 생성에 실패한 리포트는 title이 null일 수 있다.
 */
public record AiReportDetailResult(Long id, String videoObjectKey, String title, AiReportStatus status,
		AiReportContent content, LocalDateTime createdAt) {
}
