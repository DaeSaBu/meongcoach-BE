package com.daesabu.meongcoach.ai.application;

import com.daesabu.meongcoach.ai.application.provided.AiReportGenerator;
import com.daesabu.meongcoach.ai.application.required.AiReportRepository;
import com.daesabu.meongcoach.ai.application.required.VideoAnalyzer;
import com.daesabu.meongcoach.ai.domain.AiReport;
import com.daesabu.meongcoach.ai.domain.AiReportCreateCommand;
import com.daesabu.meongcoach.media.application.provided.VideoDownloadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.VideoDownloadUrlResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 업로드된 영상의 다운로드 URL을 발급받아 분석기에 넘기고, 결과를 리포트로 저장한다.
 * 영상 분석이 수십 초 이상 걸려 클래스 기본 @Transactional(readOnly = true)를 두면 분석 내내
 * DB 커넥션을 점유하므로 트랜잭션 없이 두고, 저장은 리포지토리의 자체 트랜잭션에 맡긴다.
 */
@Service
@RequiredArgsConstructor
public class AiReportGenerateService implements AiReportGenerator {

	private final VideoDownloadUrlIssuer videoDownloadUrlIssuer;
	private final VideoAnalyzer videoAnalyzer;
	private final AiReportRepository aiReportRepository;

	@Override
	public void generate(String objectKey) {
		VideoDownloadUrlResult downloadUrl = videoDownloadUrlIssuer.issue(objectKey);
		// SQS는 at-least-once라 같은 영상이 다시 전달될 수 있다. 만료 없는 공개 URL을 기준으로 중복을 거른다
		if (aiReportRepository.existsByVideoUrl(downloadUrl.publicUrl())) {
			return;
		}

		String content = videoAnalyzer.analyze(downloadUrl.downloadUrl());

		aiReportRepository.save(AiReport.create(
				new AiReportCreateCommand(downloadUrl.ownerUserId(), downloadUrl.publicUrl(), content)));
	}
}
