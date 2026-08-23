package com.daesabu.meongcoach.ai.application.provided;

import com.daesabu.meongcoach.ai.domain.AiReportStatus;
import java.time.LocalDateTime;

/**
 * AI 리포트 목록 항목.
 * 본문은 크기 상한이 없는 통 텍스트라 목록에서는 제외하고 상세 조회에서만 내린다.
 * status가 COMPLETED가 아니면 title은 null이며, COMPLETED여도 제목 생성에 실패한 리포트는 null일 수 있다.
 */
public record AiReportResult(Long id, String videoObjectKey, String title, AiReportStatus status,
		LocalDateTime createdAt) {
}
