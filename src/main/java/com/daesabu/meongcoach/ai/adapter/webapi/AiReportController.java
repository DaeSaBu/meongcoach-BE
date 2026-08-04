package com.daesabu.meongcoach.ai.adapter.webapi;

import com.daesabu.meongcoach.ai.adapter.webapi.dto.AiReportDetailResponse;
import com.daesabu.meongcoach.ai.adapter.webapi.dto.AiReportListResponse;
import com.daesabu.meongcoach.ai.adapter.webapi.dto.AiReportTrialResponse;
import com.daesabu.meongcoach.ai.adapter.webapi.dto.AiReportVideoUploadUrlRequest;
import com.daesabu.meongcoach.ai.adapter.webapi.dto.AiReportVideoUploadUrlResponse;
import com.daesabu.meongcoach.ai.application.provided.AiReportFinder;
import com.daesabu.meongcoach.ai.application.provided.AiReportTrialFinder;
import com.daesabu.meongcoach.ai.application.provided.AiReportVideoUploadUrlIssuer;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/reports")
@RequiredArgsConstructor
public class AiReportController {

	private final AiReportFinder aiReportFinder;
	private final AiReportVideoUploadUrlIssuer aiReportVideoUploadUrlIssuer;
	private final AiReportTrialFinder aiReportTrialFinder;

	@GetMapping
	public AiReportListResponse findReports(@CurrentUserId Long userId) {
		return AiReportListResponse.from(aiReportFinder.findReports(userId));
	}

	@GetMapping("/{reportId}")
	public AiReportDetailResponse findReport(@CurrentUserId Long userId, @PathVariable Long reportId) {
		return AiReportDetailResponse.from(aiReportFinder.findReport(userId, reportId));
	}

	@PostMapping("/video-upload-urls")
	public AiReportVideoUploadUrlResponse issueVideoUploadUrl(@CurrentUserId Long userId,
	                                                          @Valid @RequestBody AiReportVideoUploadUrlRequest request) {
		return AiReportVideoUploadUrlResponse.from(
				aiReportVideoUploadUrlIssuer.issue(userId, request.contentType(), request.fileSizeBytes()));
	}

	@GetMapping("/trials")
	public AiReportTrialResponse findTrial(@CurrentUserId Long userId) {
		return AiReportTrialResponse.from(aiReportTrialFinder.findTrial(userId));
	}
}
