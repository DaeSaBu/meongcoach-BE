package com.daesabu.meongcoach.ai.application;

import com.daesabu.meongcoach.ai.application.provided.AiAnalysisRegister;
import com.daesabu.meongcoach.ai.application.provided.AiAnalysisRegisterResult;
import com.daesabu.meongcoach.ai.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.ai.application.provided.AiVideoUploadUrlIssuer;
import com.daesabu.meongcoach.ai.application.provided.AiVideoUploadUrlResult;
import com.daesabu.meongcoach.ai.application.provided.dto.AiAnalysisRegisterRequest;
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
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class AiVideoUploadUrlService implements AiVideoUploadUrlIssuer, AiAnalysisRegister {

	private final MediaUploadUrlIssuer mediaUploadUrlIssuer;
	private final AiTrialFinder aiTrialFinder;
	private final AiAnalysisRepository aiAnalysisRepository;

	@Override
	@Transactional
	public AiVideoUploadUrlResult issue(Long userId, AiVideoUploadUrlRequest request) {
		IssuedUpload issued = issueUpload(userId, request.contentType(), request.fileSizeBytes(),
				request.durationSeconds(), null);
		MediaUploadUrlResult upload = issued.upload();
		return new AiVideoUploadUrlResult(issued.analysis().getId(), upload.uploadUrl(), upload.publicUrl(),
				upload.objectKey(), upload.expiresInSeconds());
	}

	@Override
	@Transactional
	public AiAnalysisRegisterResult register(Long userId, AiAnalysisRegisterRequest request) {
		IssuedUpload issued = issueUpload(userId, request.contentType(), request.fileSizeBytes(),
				request.durationSeconds(), request.context());
		MediaUploadUrlResult upload = issued.upload();
		return new AiAnalysisRegisterResult(issued.analysis().getId(), upload.uploadUrl(), upload.objectKey(),
				upload.expiresInSeconds());
	}

	private IssuedUpload issueUpload(Long userId, String contentType, Long fileSizeBytes, Long durationSeconds,
			String context) {
		AiTrial trial = aiTrialFinder.findTrial(userId);
		if (!trial.isAvailable()) {
			throw new AiReportTrialExceededException();
		}
		AiVideo.validateDuration(durationSeconds);

		MediaUploadRequest uploadRequest = MediaUploadRequest.video(AiVideo.SCOPE, userId, contentType,
				fileSizeBytes, AiVideo.MAX_FILE_SIZE_BYTES);
		MediaUploadUrlResult upload = mediaUploadUrlIssuer.issueToGcs(uploadRequest);
		Instant uploadExpiresAt = Instant.now().plusSeconds(upload.expiresInSeconds());
		AiAnalysisUploadCommand command =
				new AiAnalysisUploadCommand(userId, upload.objectKey(), uploadExpiresAt, context);
		AiAnalysis analysis = aiAnalysisRepository.save(AiAnalysis.uploading(command));
		return new IssuedUpload(analysis, upload);
	}

	private record IssuedUpload(AiAnalysis analysis, MediaUploadUrlResult upload) {
	}
}
