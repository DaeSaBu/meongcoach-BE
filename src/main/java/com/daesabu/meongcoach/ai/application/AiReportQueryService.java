package com.daesabu.meongcoach.ai.application;

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

/**
 * AI 리포트 목록·상세 조회 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiReportQueryService implements AiReportFinder {

	private final AiReportRepository aiReportRepository;

	@Override
	public List<AiReportView> findReports(Long userId) {
		return aiReportRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
				.map(report -> new AiReportView(report.getId(), report.getVideoObjectKey(), report.getCreatedAt()))
				.toList();
	}

	@Override
	public AiReportDetailView findReport(Long userId, Long reportId) {
		AiReport report = aiReportRepository.findByIdAndUserId(reportId, userId)
				.orElseThrow(() -> new AiReportNotFoundException(reportId));
		return new AiReportDetailView(report.getId(), report.getVideoObjectKey(), report.getContent(),
				report.getCreatedAt());
	}
}
