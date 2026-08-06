package com.daesabu.meongcoach.ai.application;

import com.daesabu.meongcoach.ai.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.ai.application.provided.AiTrialView;
import com.daesabu.meongcoach.ai.application.provided.AiVideoUploadUrlIssuer;
import com.daesabu.meongcoach.ai.application.provided.AiVideoUploadUrlView;
import com.daesabu.meongcoach.ai.application.required.AiReportRepository;
import com.daesabu.meongcoach.ai.domain.exception.AiReportTrialExceededException;
import com.daesabu.meongcoach.ai.domain.vo.AiTrial;
import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 리포트 무료 체험 횟수를 검증·조회하고, 한도 내에서 영상 업로드 URL 발급을 media 모듈에 위임한다.
 * MVP라 별도 카운터 없이 생성된 리포트 수(count)를 한도 기준으로 쓴다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiReportTrialService implements AiVideoUploadUrlIssuer, AiTrialFinder {

	// 모듈 경계를 넘는 값이라 media의 enum 대신 문자열을 쓴다. AI 리포트 영상은 훈련 영상으로 고정한다
	private static final String UPLOAD_TARGET = "TRAINING_VIDEO";

	private final AiReportRepository aiReportRepository;
	private final VideoUploadUrlIssuer videoUploadUrlIssuer;

	@Override
	public AiVideoUploadUrlView issue(Long userId, String contentType, long fileSizeBytes) {
		AiTrial trial = AiTrial.of(aiReportRepository.countByUserId(userId));
		if (!trial.isAvailable()) {
			throw new AiReportTrialExceededException();
		}

		VideoUploadUrlResult result = videoUploadUrlIssuer.issue(userId, UPLOAD_TARGET, contentType, fileSizeBytes);
		return new AiVideoUploadUrlView(result.uploadUrl(), result.publicUrl(), result.objectKey(),
				result.expiresInSeconds());
	}

	@Override
	public AiTrialView findTrial(Long userId) {
		AiTrial trial = AiTrial.of(aiReportRepository.countByUserId(userId));
		return new AiTrialView(trial.usedCount(), trial.maxCount(), trial.remainingCount());
	}
}
