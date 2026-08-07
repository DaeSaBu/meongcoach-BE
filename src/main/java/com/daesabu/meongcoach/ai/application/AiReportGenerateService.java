package com.daesabu.meongcoach.ai.application;

import com.daesabu.meongcoach.ai.application.provided.AiReportGenerator;
import com.daesabu.meongcoach.ai.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.ai.application.required.AiReportRepository;
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
 * 업로드된 영상의 다운로드 URL을 발급받아 분석기에 넘기고, 결과를 리포트로 저장한다.
 * 영상 분석이 수십 초 이상 걸려 클래스 기본 @Transactional(readOnly = true)를 두면 분석 내내
 * DB 커넥션을 점유하므로 트랜잭션 없이 두고, 저장은 리포지토리의 자체 트랜잭션에 맡긴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiReportGenerateService implements AiReportGenerator {

	private final VideoDownloadUrlIssuer videoDownloadUrlIssuer;
	private final VideoAnalyzer videoAnalyzer;
	private final AiReportRepository aiReportRepository;
	private final AiTrialFinder aiTrialFinder;

	@Override
	public void generate(String objectKey) {
		// SQS는 at-least-once라 같은 영상이 다시 전달될 수 있다. 객체 키를 기준으로 먼저 걸러 불필요한 URL 발급을 막는다
		if (aiReportRepository.existsByVideoObjectKey(objectKey)) {
			return;
		}

		VideoDownloadUrlResult downloadUrl = videoDownloadUrlIssuer.issue(objectKey);
		// 발급 시점의 한도 검증은 비동기 특성상 URL 연속 발급으로 우회될 수 있어, 생성 직전에 한 번 더 막는다.
		// 예외를 던지면 SQS가 재전달하므로 로그만 남기고 정상 반환한다
		AiTrial trial = aiTrialFinder.findTrial(downloadUrl.ownerUserId());
		if (!trial.isAvailable()) {
			log.warn("무료 체험 횟수를 초과한 영상이라 리포트 생성을 건너뛴다: {}", objectKey);
			return;
		}

		String content;
		try {
			content = videoAnalyzer.analyze(downloadUrl.s3Uri());
		}
		catch (Exception e) {
			// 예외를 던지면 SQS가 같은 메시지를 계속 재전달하므로, 로그만 남기고 정상 반환한다
			log.error("영상 분석에 실패해 리포트 생성을 건너뛴다: {}", objectKey, e);
			return;
		}

		aiReportRepository.save(AiReport.create(
				new AiReportCreateCommand(downloadUrl.ownerUserId(), objectKey, content)));
	}
}
