package com.daesabu.meongcoach.analysis.adapter.webapi.dto;

import com.daesabu.meongcoach.analysis.application.provided.AiReportResult;
import java.util.List;

/**
 * AI 리포트 목록 조회 응답.
 */
public record AiReportListResponse(List<AiReportResponse> reports) {

	public static AiReportListResponse from(List<AiReportResult> results) {
		List<AiReportResponse> reports = results.stream()
				.map(AiReportResponse::from)
				.toList();
		return new AiReportListResponse(reports);
	}
}
