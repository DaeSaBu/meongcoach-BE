package com.daesabu.meongcoach.ai.adapter.webapi;

import com.daesabu.meongcoach.ai.adapter.webapi.dto.AiReportListResponse;
import com.daesabu.meongcoach.ai.adapter.webapi.dto.AiTrialResponse;
import com.daesabu.meongcoach.ai.application.provided.AiReportFinder;
import com.daesabu.meongcoach.ai.application.provided.AiTrialFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * JMeter 부하 측정용 임시 API. 인증 없이 회원 ID를 쿼리 파라미터로 받아 AiController와 같은 조회 로직을 실행한다.
 * 측정이 끝나면 SecurityConfig의 /api/test/** 허용과 함께 제거한다.
 */
@RestController
@RequestMapping("/api/test/ai")
@RequiredArgsConstructor
public class AiLoadTestController {

	private final AiReportFinder aiReportFinder;
	private final AiTrialFinder aiTrialFinder;

	@GetMapping("/reports")
	public AiReportListResponse findReports(@RequestParam Long userId) {
		return AiReportListResponse.from(aiReportFinder.findReports(userId));
	}

	@GetMapping("/trial")
	public AiTrialResponse findTrial(@RequestParam Long userId) {
		return AiTrialResponse.from(aiTrialFinder.findTrial(userId));
	}
}
