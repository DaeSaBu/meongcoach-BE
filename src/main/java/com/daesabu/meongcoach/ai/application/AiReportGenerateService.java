package com.daesabu.meongcoach.ai.application;

import com.daesabu.meongcoach.ai.application.provided.AiReportGenerator;
import com.daesabu.meongcoach.ai.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.ai.application.required.AiReportRepository;
import com.daesabu.meongcoach.ai.application.required.ReportTitleGenerator;
import com.daesabu.meongcoach.ai.application.required.VideoAnalyzer;
import com.daesabu.meongcoach.ai.domain.AiReport;
import com.daesabu.meongcoach.ai.domain.exception.ReportTitleGenerationFailedException;
import com.daesabu.meongcoach.ai.domain.exception.VideoAnalysisFailedException;
import com.daesabu.meongcoach.ai.domain.vo.AiTrial;
import com.daesabu.meongcoach.media.application.provided.VideoDownloadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.VideoDownloadUrlResult;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 업로드 이벤트에서 받은 객체 키로 업로드 URL 발급 시 만든 UPLOADING 리포트를 찾아 PENDING으로 전이하고,
 * presigned 다운로드 URL을 분석기에 넘겨 결말에 따라 같은 row를 COMPLETED 또는 FAILED_* 로 전이한다.
 * 영상 분석이 수십 초 이상 걸려 클래스 기본 @Transactional(readOnly = true)를 두면 분석 내내
 * DB 커넥션을 점유하므로 트랜잭션 없이 두고, 상태 전이는 리포지토리 save(merge)의 자체 트랜잭션에 맡긴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiReportGenerateService implements AiReportGenerator {

	private final VideoDownloadUrlIssuer videoDownloadUrlIssuer;
	private final VideoAnalyzer videoAnalyzer;
	private final ReportTitleGenerator reportTitleGenerator;
	private final AiReportRepository aiReportRepository;
	private final AiTrialFinder aiTrialFinder;

	@Override
	public void generate(String objectKey) {
		AiReport report = aiReportRepository.findByVideoObjectKey(objectKey).orElse(null);
		if (report == null) {
			// 업로드 URL 발급 없이 올라온 객체(또는 발급 기록 도입 전 분량)는 소유자 row가 없어 분석하지 않는다
			log.warn("업로드 URL 발급 기록이 없는 영상이라 리포트 생성을 건너뛴다: {}", objectKey);
			return;
		}
		if (!report.isUploading()) {
			// SQS at-least-once 중복 전달. 이미 분석을 시작했거나 끝난 row다
			return;
		}

		// 다운로드 URL 발급(객체 키 검증)이 실패하면 row는 UPLOADING으로 남아 만료 뒤 FAILED_UPLOAD로 조회된다
		VideoDownloadUrlResult downloadUrl = videoDownloadUrlIssuer.issue(objectKey);

		report.startAnalysis();
		aiReportRepository.save(report);

		try {
			recordOutcome(report, downloadUrl.downloadUrl());
		}
		catch (Exception e) {
			log.error("예상하지 못한 오류로 리포트 생성에 실패했다: {}", objectKey, e);
			recordFailure(report, AiReport::failUnexpectedly);
		}
	}

	private void recordOutcome(AiReport report, String downloadUrl) {
		if (isTrialExhausted(report.getUserId())) {
			recordFailure(report, AiReport::failByTrialExceeded);
			return;
		}

		String content = analyzeOrNull(report.getVideoObjectKey(), downloadUrl);
		if (content == null) {
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

	private String analyzeOrNull(String objectKey, String downloadUrl) {
		try {
			return videoAnalyzer.analyze(downloadUrl);
		}
		catch (VideoAnalysisFailedException e) {
			log.error("영상 분석에 실패해 리포트 생성을 건너뛴다: {}", objectKey, e);
			return null;
		}
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
