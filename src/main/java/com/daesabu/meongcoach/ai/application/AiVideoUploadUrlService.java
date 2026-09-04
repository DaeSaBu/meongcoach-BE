package com.daesabu.meongcoach.ai.application;

import com.daesabu.meongcoach.ai.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.ai.application.provided.AiVideoUploadUrlIssuer;
import com.daesabu.meongcoach.ai.application.provided.AiVideoUploadUrlResult;
import com.daesabu.meongcoach.ai.application.required.AiReportRepository;
import com.daesabu.meongcoach.ai.domain.AiReport;
import com.daesabu.meongcoach.ai.domain.AiReportUploadCommand;
import com.daesabu.meongcoach.ai.domain.exception.AiReportTrialExceededException;
import com.daesabu.meongcoach.ai.domain.vo.AiTrial;
import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlResult;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 리포트 무료 체험 한도를 검증하고, 한도 내에서 영상 업로드 URL 발급을 media 모듈에 위임한다.
 * 발급과 동시에 UPLOADING 리포트를 저장해, 앱이 객체 키를 잃어도 목록 조회만으로 진행 중인 분석을 찾을 수 있게 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiVideoUploadUrlService implements AiVideoUploadUrlIssuer {

	// 모듈 경계를 넘는 값이라 media의 enum 대신 문자열을 쓴다. AI 리포트 영상은 훈련 영상으로 고정한다
	private static final String UPLOAD_TARGET = "TRAINING_VIDEO";

	private final VideoUploadUrlIssuer videoUploadUrlIssuer;
	private final AiTrialFinder aiTrialFinder;
	private final AiReportRepository aiReportRepository;

	@Override
	@Transactional
	public AiVideoUploadUrlResult issue(Long userId, String contentType, long fileSizeBytes) {
		AiTrial trial = aiTrialFinder.findTrial(userId);
		if (!trial.isAvailable()) {
			throw new AiReportTrialExceededException();
		}

		VideoUploadUrlResult result = videoUploadUrlIssuer.issue(userId, UPLOAD_TARGET, contentType, fileSizeBytes);
		LocalDateTime uploadExpiresAt = LocalDateTime.now().plusSeconds(result.expiresInSeconds());
		AiReport report = aiReportRepository.save(
				AiReport.uploading(new AiReportUploadCommand(userId, result.objectKey(), uploadExpiresAt)));
		return new AiVideoUploadUrlResult(report.getId(), result.uploadUrl(), result.publicUrl(), result.objectKey(),
				result.expiresInSeconds());
	}
}
