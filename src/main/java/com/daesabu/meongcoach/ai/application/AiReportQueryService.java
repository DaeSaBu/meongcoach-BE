package com.daesabu.meongcoach.ai.application;

import com.daesabu.meongcoach.ai.application.provided.AiReportContent;
import com.daesabu.meongcoach.ai.application.provided.AiReportDetailResult;
import com.daesabu.meongcoach.ai.application.provided.AiReportFinder;
import com.daesabu.meongcoach.ai.application.provided.AiReportResult;
import com.daesabu.meongcoach.ai.application.required.AiReportRepository;
import com.daesabu.meongcoach.ai.domain.AiReport;
import com.daesabu.meongcoach.ai.domain.exception.AiReportNotFoundException;
import java.time.LocalDateTime;
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

	// 업로드가 만료된 UPLOADING은 조회 시점에 FAILED_UPLOAD로 파생하므로 저장된 status 대신 statusAt(now)를 내린다
	@Override
	public List<AiReportResult> findReports(Long userId) {
		LocalDateTime now = LocalDateTime.now();
		return aiReportRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
				.map(report -> new AiReportResult(report.getId(), report.getVideoObjectKey(), report.getTitle(),
						report.statusAt(now), report.getCreatedAt()))
				.toList();
	}

	@Override
	public AiReportDetailResult findReport(Long userId, Long reportId) {
		AiReport report = aiReportRepository.findByIdAndUserId(reportId, userId)
				.orElseThrow(() -> new AiReportNotFoundException(reportId));
		AiReportContent content = parseContentOrNull(report.getContent());
		LocalDateTime now = LocalDateTime.now();
		return new AiReportDetailResult(report.getId(), report.getVideoObjectKey(), report.getTitle(),
				report.statusAt(now), content, report.getCreatedAt());
	}

	// COMPLETED가 아닌 리포트는 본문이 없다. 본문이 있으면 저장 시점에 구조를 검증한 JSON이라 파싱 실패는 데이터 손상으로 보고 그대로 던진다
	private AiReportContent parseContentOrNull(String content) {
		if (content == null) {
			return null;
		}
		return objectMapper.readValue(content, AiReportContent.class);
	}
}
