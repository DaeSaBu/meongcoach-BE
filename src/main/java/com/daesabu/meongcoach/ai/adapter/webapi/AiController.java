package com.daesabu.meongcoach.ai.adapter.webapi;

import com.daesabu.meongcoach.ai.adapter.webapi.dto.AiAnalysisRegisterResponse;
import com.daesabu.meongcoach.ai.adapter.webapi.dto.AiReportDetailResponse;
import com.daesabu.meongcoach.ai.adapter.webapi.dto.AiReportListResponse;
import com.daesabu.meongcoach.ai.adapter.webapi.dto.AiTrialResponse;
import com.daesabu.meongcoach.ai.adapter.webapi.dto.AiVideoUploadUrlResponse;
import com.daesabu.meongcoach.ai.application.provided.AiAnalysisRegister;
import com.daesabu.meongcoach.ai.application.provided.AiReportFinder;
import com.daesabu.meongcoach.ai.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.ai.application.provided.AiVideoUploadUrlIssuer;
import com.daesabu.meongcoach.ai.application.provided.dto.AiAnalysisRegisterRequest;
import com.daesabu.meongcoach.ai.application.provided.dto.AiVideoUploadUrlRequest;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

	private final AiReportFinder aiReportFinder;
	private final AiVideoUploadUrlIssuer aiVideoUploadUrlIssuer;
	private final AiTrialFinder aiTrialFinder;
	private final AiAnalysisRegister aiAnalysisRegister;

	@GetMapping("/reports")
	public AiReportListResponse findReports(@CurrentUserId Long userId) {
		return AiReportListResponse.from(aiReportFinder.findReports(userId));
	}

	@GetMapping("/reports/{reportId}")
	public AiReportDetailResponse findReport(@CurrentUserId Long userId, @PathVariable Long reportId) {
		return AiReportDetailResponse.from(aiReportFinder.findReport(userId, reportId));
	}

	@PostMapping("/presigned-urls")
	public AiVideoUploadUrlResponse issueVideoUploadUrl(@CurrentUserId Long userId,
	                                                    @Valid @RequestBody AiVideoUploadUrlRequest request) {
		return AiVideoUploadUrlResponse.from(aiVideoUploadUrlIssuer.issue(userId, request));
	}

	@PostMapping("/analyses")
	@ResponseStatus(HttpStatus.CREATED)
	public AiAnalysisRegisterResponse registerAnalysis(@CurrentUserId Long userId,
	                                                   @Valid @RequestBody AiAnalysisRegisterRequest request) {
		return AiAnalysisRegisterResponse.from(aiAnalysisRegister.register(userId, request));
	}

	@GetMapping("/trial")
	public AiTrialResponse findTrial(@CurrentUserId Long userId) {
		return AiTrialResponse.from(aiTrialFinder.findTrial(userId));
	}
}
