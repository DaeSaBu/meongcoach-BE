package com.daesabu.meongcoach.ai.application;

import com.daesabu.meongcoach.ai.application.provided.AiReportGenerator;
import com.daesabu.meongcoach.ai.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.ai.application.required.AiReportRepository;
import com.daesabu.meongcoach.ai.application.required.ReportTitleGenerator;
import com.daesabu.meongcoach.ai.application.required.VideoAnalyzer;
import com.daesabu.meongcoach.ai.domain.AiReport;
import com.daesabu.meongcoach.ai.domain.AiTrial;
import com.daesabu.meongcoach.ai.domain.AiVideo;
import com.daesabu.meongcoach.ai.domain.exception.ReportTitleGenerationFailedException;
import com.daesabu.meongcoach.ai.domain.exception.VideoAnalysisFailedException;
import com.daesabu.meongcoach.media.application.provided.MediaStorageUriResolver;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReportGenerateService implements AiReportGenerator {

	private final MediaStorageUriResolver mediaStorageUriResolver;
	private final VideoAnalyzer videoAnalyzer;
	private final ReportTitleGenerator reportTitleGenerator;
	private final AiReportRepository aiReportRepository;
	private final AiTrialFinder aiTrialFinder;

	@Override
	public void generate(String objectKey) {
		AiReport report = aiReportRepository.findByVideoObjectKey(objectKey).orElse(null);
		if (report == null) {
			log.warn("객체 키에 대응하는 AiReport가 없다: {}", objectKey);
			return;
		}
		if (!report.isUploading()) {
			return;
		}

		String videoUri = mediaStorageUriResolver.resolve(AiVideo.SCOPE, objectKey);

		report.startAnalysis();
		aiReportRepository.save(report);

		try {
			recordOutcome(report, videoUri);
		}
		catch (Exception e) {
			log.error("예상하지 못한 오류로 리포트 생성에 실패했다: {}", objectKey, e);
			recordFailure(report, AiReport::failUnexpectedly);
		}
	}

	private void recordOutcome(AiReport report, String videoUri) {
		if (isTrialExhausted(report.getUserId())) {
			recordFailure(report, AiReport::failByTrialExceeded);
			return;
		}

		String content;
		try {
			content = videoAnalyzer.analyze(videoUri);
		}
		catch (VideoAnalysisFailedException e) {
			log.error("영상 분석에 실패해 리포트 생성을 건너뛴다: {}", report.getVideoObjectKey(), e);
			recordFailure(report, AiReport::failByAnalysis);
			return;
		}

		recordCompletion(report, content);
	}

	private boolean isTrialExhausted(Long userId) {
		AiTrial trial = aiTrialFinder.findTrial(userId);
		return !trial.isAvailable();
	}

	private void recordCompletion(AiReport report, String content) {
		String title = generateTitleOrNull(report.getVideoObjectKey(), content);
		report.complete(title, content);
		aiReportRepository.save(report);
	}

	private String generateTitleOrNull(String objectKey, String content) {
		try {
			return reportTitleGenerator.generateTitle(content);
		}
		catch (ReportTitleGenerationFailedException e) {
			log.warn("리포트 제목 생성에 실패해 제목 없이 저장한다: {}", objectKey, e);
			return null;
		}
	}

	private void recordFailure(AiReport report, Consumer<AiReport> transition) {
		transition.accept(report);
		aiReportRepository.save(report);
	}
}
