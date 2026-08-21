package com.daesabu.meongcoach.ai.application.provided;

import com.daesabu.meongcoach.ai.domain.AiReportStatus;

/**
 * AI 리포트 상태 폴링 결과. 잦은 호출을 전제로 식별자와 상태만 담고 제목·본문은 싣지 않는다.
 */
public record AiReportStatusResult(Long id, AiReportStatus status) {
}
