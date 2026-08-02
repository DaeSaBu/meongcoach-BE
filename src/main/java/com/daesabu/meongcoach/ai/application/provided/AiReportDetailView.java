package com.daesabu.meongcoach.ai.application.provided;

import java.time.LocalDateTime;

/**
 * AI 리포트 상세 조회 결과. 리포트 본문을 포함한다.
 */
public record AiReportDetailView(Long id, String videoObjectKey, String content, LocalDateTime createdAt) {
}
