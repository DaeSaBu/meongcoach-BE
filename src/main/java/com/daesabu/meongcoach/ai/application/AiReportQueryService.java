package com.daesabu.meongcoach.ai.application;

import com.daesabu.meongcoach.ai.application.provided.AiReportContent;
import com.daesabu.meongcoach.ai.application.provided.AiReportDetailResult;
import com.daesabu.meongcoach.ai.application.provided.AiReportFinder;
import com.daesabu.meongcoach.ai.application.provided.AiReportResult;
import com.daesabu.meongcoach.ai.application.provided.AiReportStatusResult;
import com.daesabu.meongcoach.ai.application.required.AiReportRepository;
import com.daesabu.meongcoach.ai.domain.AiReport;
import com.daesabu.meongcoach.ai.domain.exception.AiReportNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * AI 리포트 목록·상세·상태 조회 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiReportQueryService implements AiReportFinder {

	private final AiReportRepository aiReportRepository;
	private final ObjectMapper objectMapper;

	@Override
	public List<AiReportResult> findReports(Long userId) {
		return aiReportRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
				.map(report -> new AiReportResult(report.getId(), report.getVideoObjectKey(), report.getTitle(),
						report.getStatus(), report.getCreatedAt()))
				.toList();
	}

	@Override
	public AiReportDetailResult findReport(Long userId, Long reportId) {
		AiReport report = aiReportRepository.findByIdAndUserId(reportId, userId)
				.orElseThrow(() -> new AiReportNotFoundException(reportId));
		AiReportContent content = parseContentOrNull(report.getContent());
		return new AiReportDetailResult(report.getId(), report.getVideoObjectKey(), report.getTitle(),
				report.getStatus(), content, report.getCreatedAt());
	}

	@Override
	public AiReportStatusResult findReportStatus(Long userId, String videoObjectKey) {
		AiReport report = aiReportRepository.findByVideoObjectKeyAndUserId(videoObjectKey, userId)
				.orElseThrow(() -> new AiReportNotFoundException(videoObjectKey));
		return new AiReportStatusResult(report.getId(), report.getStatus());
	}

	// COMPLETED가 아닌 리포트는 본문이 없다. 본문이 있으면 저장 시점에 구조를 검증한 JSON이라 파싱 실패는 데이터 손상으로 보고 그대로 던진다
	private AiReportContent parseContentOrNull(String content) {
		if (content == null) {
			return null;
		}
		return objectMapper.readValue(content, AiReportContent.class);
	}
}
