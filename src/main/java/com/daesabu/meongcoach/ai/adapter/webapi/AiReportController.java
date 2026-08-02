package com.daesabu.meongcoach.ai.adapter.webapi;

import com.daesabu.meongcoach.ai.adapter.webapi.dto.AiReportDetailResponse;
import com.daesabu.meongcoach.ai.adapter.webapi.dto.AiReportListResponse;
import com.daesabu.meongcoach.ai.application.provided.AiReportFinder;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/reports")
@RequiredArgsConstructor
public class AiReportController {

	private final AiReportFinder aiReportFinder;

	@GetMapping
	public AiReportListResponse findReports(@CurrentUserId Long userId) {
		return AiReportListResponse.from(aiReportFinder.findReports(userId));
	}

	@GetMapping("/{reportId}")
	public AiReportDetailResponse findReport(@CurrentUserId Long userId, @PathVariable Long reportId) {
		return AiReportDetailResponse.from(aiReportFinder.findReport(userId, reportId));
	}
}
