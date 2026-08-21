package com.daesabu.meongcoach.ai.application;

import com.daesabu.meongcoach.ai.application.provided.AiReportGenerator;
import com.daesabu.meongcoach.ai.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.ai.application.required.AiReportRepository;
import com.daesabu.meongcoach.ai.application.required.ReportTitleGenerator;
import com.daesabu.meongcoach.ai.application.required.VideoAnalyzer;
import com.daesabu.meongcoach.ai.domain.AiReport;
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

		// 분석기가 이 presigned URL로 영상을 직접 읽는다. 소유자 ID도 같은 결과에서 얻는다.
		// 키 형식 위반은 소유자를 알 수 없어 row를 남길 수 없으므로 그대로 던진다 (컨슈머가 warn 로그로 버린다)
		VideoDownloadUrlResult downloadUrl = videoDownloadUrlIssuer.issue(objectKey);

		// 소유자를 알게 된 즉시 PENDING row를 남겨, 이후 어떤 결말이든 같은 row의 상태로 기록한다
		AiReport report = aiReportRepository.save(AiReport.pending(downloadUrl.ownerUserId(), objectKey));

		// PENDING row가 생긴 뒤의 예외는 전부 실패 결말로 남긴다. 예외를 던지면 SQS가 같은 메시지를 계속 재전달한다
		try {
			analyzeAndComplete(report, downloadUrl.downloadUrl());
		}
		catch (Exception e) {
			log.error("예상하지 못한 오류로 리포트 생성에 실패했다: {}", objectKey, e);
			recordFailure(report, AiReport::failUnexpectedly);
		}
	}

	private boolean isDuplicateAnalysis(String objectKey) {
		return aiReportRepository.existsByVideoObjectKey(objectKey);
	}

	private void analyzeAndComplete(AiReport report, String downloadUrl) {
		String objectKey = report.getVideoObjectKey();

		AiTrial trial = aiTrialFinder.findTrial(report.getUserId());
		if (!trial.isAvailable()) {
			log.warn("무료 체험 횟수를 초과한 영상이라 리포트 생성을 건너뛴다: {}", objectKey);
			recordFailure(report, AiReport::failByTrialExceeded);
			return;
		}

		String content = analyzeOrNull(objectKey, downloadUrl);
		if (content == null) {
			recordFailure(report, AiReport::failByAnalysis);
			return;
		}

		String title = generateTitleOrNull(objectKey, content);
		report.complete(title, content);
		aiReportRepository.save(report);
	}

	private String analyzeOrNull(String objectKey, String downloadUrl) {
		try {
			return videoAnalyzer.analyze(downloadUrl);
		}
		catch (Exception e) {
			log.error("영상 분석에 실패해 리포트 생성을 건너뛴다: {}", objectKey, e);
			return null;
		}
	}

	// 제목은 부가 정보라 생성에 실패해도 리포트 저장은 계속한다
	private String generateTitleOrNull(String objectKey, String content) {
		try {
			return reportTitleGenerator.generateTitle(content);
		}
		catch (Exception e) {
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
