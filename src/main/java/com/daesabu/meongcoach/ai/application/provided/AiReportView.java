package com.daesabu.meongcoach.ai.application.provided;

import java.time.LocalDateTime;

/**
 * AI 리포트 목록 항목.
 * 본문은 크기 상한이 없는 통 텍스트라 목록에서는 제외하고 상세 조회에서만 내린다.
 */
public record AiReportView(Long id, String videoObjectKey, LocalDateTime createdAt) {
}
