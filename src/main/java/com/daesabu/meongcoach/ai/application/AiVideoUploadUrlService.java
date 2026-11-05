package com.daesabu.meongcoach.ai.application;

import com.daesabu.meongcoach.ai.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.ai.application.provided.AiVideoUploadUrlIssuer;
import com.daesabu.meongcoach.ai.application.provided.AiVideoUploadUrlResult;
import com.daesabu.meongcoach.ai.application.provided.dto.AiVideoUploadUrlRequest;
import com.daesabu.meongcoach.ai.application.required.AiAnalysisRepository;
import com.daesabu.meongcoach.ai.domain.AiAnalysis;
import com.daesabu.meongcoach.ai.domain.AiAnalysisUploadCommand;
import com.daesabu.meongcoach.ai.domain.AiTrial;
import com.daesabu.meongcoach.ai.domain.AiVideo;
import com.daesabu.meongcoach.ai.domain.exception.AiReportTrialExceededException;
import com.daesabu.meongcoach.media.application.provided.MediaUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadRequest;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class AiVideoUploadUrlService implements AiVideoUploadUrlIssuer {

	private final MediaUploadUrlIssuer mediaUploadUrlIssuer;
	private final AiTrialFinder aiTrialFinder;
	private final AiAnalysisRepository aiAnalysisRepository;

	@Override
	@Transactional
	public AiVideoUploadUrlResult issue(Long userId, AiVideoUploadUrlRequest request) {
		AiTrial trial = aiTrialFinder.findTrial(userId);
		if (!trial.isAvailable()) {
			throw new AiReportTrialExceededException();
		}
		AiVideo.validateDuration(request.durationSeconds());

		MediaUploadRequest uploadRequest = MediaUploadRequest.video(AiVideo.SCOPE, userId, request.contentType(),
				request.fileSizeBytes(), AiVideo.MAX_FILE_SIZE_BYTES);
		MediaUploadUrlResult result = mediaUploadUrlIssuer.issueToGcs(uploadRequest);
		LocalDateTime uploadExpiresAt = LocalDateTime.now().plusSeconds(result.expiresInSeconds());
		AiAnalysis analysis = aiAnalysisRepository.save(
				AiAnalysis.uploading(new AiAnalysisUploadCommand(userId, result.objectKey(), uploadExpiresAt)));
		return new AiVideoUploadUrlResult(analysis.getId(), result.uploadUrl(), result.publicUrl(), result.objectKey(),
				result.expiresInSeconds());
	}
}
