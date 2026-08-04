package com.daesabu.meongcoach.ai.application;

import com.daesabu.meongcoach.ai.application.provided.AiReportTrialFinder;
import com.daesabu.meongcoach.ai.application.provided.AiReportTrialView;
import com.daesabu.meongcoach.ai.application.provided.AiReportVideoUploadUrlIssuer;
import com.daesabu.meongcoach.ai.application.provided.AiReportVideoUploadUrlView;
import com.daesabu.meongcoach.ai.application.required.AiReportRepository;
import com.daesabu.meongcoach.ai.domain.AiReport;
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
public class AiReportTrialService implements AiReportVideoUploadUrlIssuer, AiReportTrialFinder {

	// 모듈 경계를 넘는 값이라 media의 enum 대신 문자열을 쓴다. AI 리포트 영상은 훈련 영상으로 고정한다
	private static final String UPLOAD_TARGET = "TRAINING_VIDEO";

	private final AiReportRepository aiReportRepository;
	private final VideoUploadUrlIssuer videoUploadUrlIssuer;

	@Override
	public AiReportVideoUploadUrlView issue(Long userId, String contentType, long fileSizeBytes) {
		AiReport.validateTrialAvailable(aiReportRepository.countByUserId(userId));

		VideoUploadUrlResult result = videoUploadUrlIssuer.issue(userId, UPLOAD_TARGET, contentType, fileSizeBytes);
		return new AiReportVideoUploadUrlView(result.uploadUrl(), result.publicUrl(), result.objectKey(),
				result.expiresInSeconds());
	}

	@Override
	public AiReportTrialView findTrial(Long userId) {
		int usedCount = Math.toIntExact(aiReportRepository.countByUserId(userId));
		// 발급~생성이 비동기라 한도를 넘겨 저장됐을 수 있으므로 남은 횟수는 음수가 되지 않게 막는다
		int remainingCount = Math.max(0, AiReport.MAX_TRIAL_COUNT - usedCount);
		return new AiReportTrialView(usedCount, AiReport.MAX_TRIAL_COUNT, remainingCount);
	}
}
