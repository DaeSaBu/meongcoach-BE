package com.daesabu.meongcoach.ai.application.provided;

import java.util.List;

/**
 * AI 리포트 조회 능력.
 */
public interface AiReportFinder {

	/**
	 * 사용자의 리포트를 생성 시각 내림차순으로 조회한다. 없으면 빈 리스트를 반환한다.
	 */
	List<AiReportResult> findReports(Long userId);

	/**
	 * 리포트 하나를 본문과 함께 조회한다.
	 * 없거나 본인 소유가 아니면 {@code AiReportNotFoundException}을 던진다.
	 */
	AiReportDetailResult findReport(Long userId, Long reportId);
}
