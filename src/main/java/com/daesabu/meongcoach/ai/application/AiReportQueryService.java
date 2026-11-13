package com.daesabu.meongcoach.ai.application;

import com.daesabu.meongcoach.ai.application.provided.AiReportContent;
import com.daesabu.meongcoach.ai.application.provided.AiReportDetailResult;
import com.daesabu.meongcoach.ai.application.provided.AiReportFinder;
import com.daesabu.meongcoach.ai.application.provided.AiReportResult;
import com.daesabu.meongcoach.ai.application.required.AiAnalysisRepository;
import com.daesabu.meongcoach.ai.domain.AiAnalysis;
import com.daesabu.meongcoach.ai.domain.AiVideo;
import com.daesabu.meongcoach.ai.domain.exception.AiReportNotFoundException;
import com.daesabu.meongcoach.media.application.provided.MediaDownloadUrlIssuer;
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
	private final MediaDownloadUrlIssuer mediaDownloadUrlIssuer;

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
		String videoUrl = issueVideoUrlOrNull(analysis);
		Instant now = Instant.now();
		return new AiReportDetailResult(analysis.getId(), analysis.getVideoObjectKey(), videoUrl, analysis.getTitle(),
				analysis.statusAt(now), content, analysis.getCreatedAt());
	}

	private String issueVideoUrlOrNull(AiAnalysis analysis) {
		if (analysis.isUploading()) {
			return null;
		}
		return mediaDownloadUrlIssuer.issue(AiVideo.SCOPE, analysis.getVideoObjectKey());
	}

	private AiReportContent parseContentOrNull(String content) {
		if (content == null) {
			return null;
		}
		return objectMapper.readValue(content, AiReportContent.class);
	}
}
