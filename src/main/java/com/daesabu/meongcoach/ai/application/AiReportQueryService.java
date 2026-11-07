package com.daesabu.meongcoach.ai.application;

import com.daesabu.meongcoach.ai.application.provided.AiReportContent;
import com.daesabu.meongcoach.ai.application.provided.AiReportDetailResult;
import com.daesabu.meongcoach.ai.application.provided.AiReportFinder;
import com.daesabu.meongcoach.ai.application.provided.AiReportResult;
import com.daesabu.meongcoach.ai.application.required.AiAnalysisRepository;
import com.daesabu.meongcoach.ai.domain.AiAnalysis;
import com.daesabu.meongcoach.ai.domain.exception.AiReportNotFoundException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiReportQueryService implements AiReportFinder {

	private final AiAnalysisRepository aiAnalysisRepository;
	private final ObjectMapper objectMapper;

	@Override
	public List<AiReportResult> findReports(Long userId) {
		Instant now = Instant.now();
		return aiAnalysisRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
				.map(analysis -> new AiReportResult(analysis.getId(), analysis.getVideoObjectKey(), analysis.getTitle(),
						analysis.statusAt(now), analysis.getCreatedAt()))
				.toList();
	}

	@Override
	public AiReportDetailResult findReport(Long userId, Long reportId) {
		AiAnalysis analysis = aiAnalysisRepository.findByIdAndUserId(reportId, userId)
				.orElseThrow(() -> new AiReportNotFoundException(reportId));
		AiReportContent content = parseContentOrNull(analysis.getContent());
		Instant now = Instant.now();
		return new AiReportDetailResult(analysis.getId(), analysis.getVideoObjectKey(), analysis.getTitle(),
				analysis.statusAt(now), content, analysis.getCreatedAt());
	}

	private AiReportContent parseContentOrNull(String content) {
		if (content == null) {
			return null;
		}
		return objectMapper.readValue(content, AiReportContent.class);
	}
}
