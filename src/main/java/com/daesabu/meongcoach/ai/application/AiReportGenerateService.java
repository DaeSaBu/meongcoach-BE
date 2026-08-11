package com.daesabu.meongcoach.ai.application;

import com.daesabu.meongcoach.ai.application.provided.AiReportGenerator;
import com.daesabu.meongcoach.ai.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.ai.application.required.AiReportRepository;
import com.daesabu.meongcoach.ai.application.required.ReportTitleGenerator;
import com.daesabu.meongcoach.ai.application.required.VideoAnalyzer;
import com.daesabu.meongcoach.ai.domain.AiReport;
import com.daesabu.meongcoach.ai.domain.AiReportCreateCommand;
import com.daesabu.meongcoach.ai.domain.vo.AiTrial;
import com.daesabu.meongcoach.media.application.provided.VideoDownloadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.VideoDownloadUrlResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 업로드 이벤트에서 받은 영상 s3 URI를 분석기에 넘기고, 결과를 리포트로 저장한다.
 * 영상 분석이 수십 초 이상 걸려 클래스 기본 @Transactional(readOnly = true)를 두면 분석 내내
 * DB 커넥션을 점유하므로 트랜잭션 없이 두고, 저장은 리포지토리의 자체 트랜잭션에 맡긴다.
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
	public void generate(String objectKey, String videoS3Uri) {
		if (aiReportRepository.existsByVideoObjectKey(objectKey)) {
			log.warn("이미 처리한 영상은 스킵한다: {}", objectKey);
			return;
		}

		VideoDownloadUrlResult downloadUrl = videoDownloadUrlIssuer.issue(objectKey);

		AiTrial trial = aiTrialFinder.findTrial(downloadUrl.ownerUserId());
		if (!trial.isAvailable()) {
			log.warn("무료 체험 횟수를 초과한 영상이라 리포트 생성을 건너뛴다: {}", objectKey);
			return;
		}

		String content;
		try {
			content = videoAnalyzer.analyze(videoS3Uri);
		}
		catch (Exception e) {
			// 예외를 던지면 SQS가 같은 메시지를 계속 재전달하므로, 로그만 남기고 정상 반환한다
			log.error("영상 분석에 실패해 리포트 생성을 건너뛴다: {}", objectKey, e);
			return;
		}

		String title = generateTitleOrNull(objectKey, content);

		aiReportRepository.save(AiReport.create(
				new AiReportCreateCommand(downloadUrl.ownerUserId(), objectKey, title, content)));
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
}
