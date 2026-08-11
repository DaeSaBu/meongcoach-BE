package com.daesabu.meongcoach.ai.application;

import com.daesabu.meongcoach.ai.application.provided.AiReportContent;
import com.daesabu.meongcoach.ai.application.provided.AiReportDetailView;
import com.daesabu.meongcoach.ai.application.provided.AiReportFinder;
import com.daesabu.meongcoach.ai.application.provided.AiReportView;
import com.daesabu.meongcoach.ai.application.required.AiReportRepository;
import com.daesabu.meongcoach.ai.domain.AiReport;
import com.daesabu.meongcoach.ai.domain.exception.AiReportNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * AI 리포트 목록·상세 조회 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiReportQueryService implements AiReportFinder {

	private final AiReportRepository aiReportRepository;
	private final ObjectMapper objectMapper;

	@Override
	public List<AiReportView> findReports(Long userId) {
		return aiReportRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
				.map(report -> new AiReportView(report.getId(), report.getVideoObjectKey(), report.getTitle(),
						report.getCreatedAt()))
				.toList();
	}

	@Override
	public AiReportDetailView findReport(Long userId, Long reportId) {
		AiReport report = aiReportRepository.findByIdAndUserId(reportId, userId)
				.orElseThrow(() -> new AiReportNotFoundException(reportId));
		// 본문은 저장 시점에 구조를 검증한 JSON이라, 파싱 실패는 데이터 손상으로 보고 그대로 던진다
		AiReportContent content = objectMapper.readValue(report.getContent(), AiReportContent.class);
		return new AiReportDetailView(report.getId(), report.getVideoObjectKey(), report.getTitle(), content,
				report.getCreatedAt());
	}
}
