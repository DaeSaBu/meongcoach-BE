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
 * 업로드 이벤트에서 받은 객체 키로 presigned 다운로드 URL을 발급해 분석기에 넘기고, 결과를 리포트로 저장한다.
 * 소유자를 확인하는 즉시 PENDING 리포트를 저장하고, 결말에 따라 같은 row를 COMPLETED 또는 FAILED_* 로 전이한다.
 * 영상 분석이 수십 초 이상 걸려 클래스 기본 @Transactional(readOnly = true)를 두면 분석 내내
 * DB 커넥션을 점유하므로 트랜잭션 없이 두고, 저장과 상태 전이는 리포지토리 save(merge)의 자체 트랜잭션에 맡긴다.
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
		if (isDuplicateAnalysis(objectKey)) {
			log.warn("이미 처리한 영상은 스킵한다: {}", objectKey);
			return;
		}

		VideoDownloadUrlResult downloadUrl = videoDownloadUrlIssuer.issue(objectKey);

		AiReport report = aiReportRepository.save(AiReport.pending(downloadUrl.ownerUserId(), objectKey));

		try {
			recordOutcome(report, downloadUrl.downloadUrl());
		}
		catch (Exception e) {
			log.error("예상하지 못한 오류로 리포트 생성에 실패했다: {}", objectKey, e);
			recordFailure(report, AiReport::failUnexpectedly);
		}
	}

	private boolean isDuplicateAnalysis(String objectKey) {
		return aiReportRepository.existsByVideoObjectKey(objectKey);
	}

	// 결말을 정해 기록한다. 모든 경로가 상태를 저장하며 끝난다
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

	// 어댑터가 번역한 연동 실패만 분석 실패로 본다. 그 외 예외는 버그로 보고 generate()의 catch에서 FAILED_UNEXPECTED로 남긴다
	private String analyzeOrNull(String objectKey, String downloadUrl) {
		try {
			return videoAnalyzer.analyze(downloadUrl);
		}
		catch (VideoAnalysisFailedException e) {
			log.error("영상 분석에 실패해 리포트 생성을 건너뛴다: {}", objectKey, e);
			return null;
		}
	}

	// 제목은 부가 정보라 생성에 실패해도 리포트 저장은 계속한다
	private String generateTitleOrNull(String objectKey, String content) {
		try {
			return reportTitleGenerator.generateTitle(content);
		}
		catch (ReportTitleGenerationFailedException e) {
			log.warn("리포트 제목 생성에 실패해 제목 없이 저장한다: {}", objectKey, e);
			return null;
		}
	}

	// 저장이 실패하면 generate()의 catch가 FAILED_UNEXPECTED로 한 번 더 시도하고, 그마저 실패하면 컨슈머가 받아 버린다
	private void recordFailure(AiReport report, Consumer<AiReport> transition) {
		transition.accept(report);
		aiReportRepository.save(report);
	}
}
