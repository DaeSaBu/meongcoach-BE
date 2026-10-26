package com.daesabu.meongcoach.ai.application;

import com.daesabu.meongcoach.ai.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.ai.application.provided.AiVideoUploadUrlIssuer;
import com.daesabu.meongcoach.ai.application.provided.AiVideoUploadUrlResult;
import com.daesabu.meongcoach.ai.application.required.AiReportRepository;
import com.daesabu.meongcoach.ai.domain.AiReport;
import com.daesabu.meongcoach.ai.domain.AiReportUploadCommand;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiVideoUploadUrlService implements AiVideoUploadUrlIssuer {

	private final MediaUploadUrlIssuer mediaUploadUrlIssuer;
	private final AiTrialFinder aiTrialFinder;
	private final AiReportRepository aiReportRepository;

	@Override
	@Transactional
	public AiVideoUploadUrlResult issue(Long userId, String contentType, long fileSizeBytes) {
		AiTrial trial = aiTrialFinder.findTrial(userId);
		if (!trial.isAvailable()) {
			throw new AiReportTrialExceededException();
		}

		MediaUploadRequest uploadRequest = MediaUploadRequest.video(AiVideo.SCOPE, userId, contentType, fileSizeBytes,
				AiVideo.MAX_FILE_SIZE_BYTES);
		MediaUploadUrlResult result = mediaUploadUrlIssuer.issue(uploadRequest);
		LocalDateTime uploadExpiresAt = LocalDateTime.now().plusSeconds(result.expiresInSeconds());
		AiReport report = aiReportRepository.save(
				AiReport.uploading(new AiReportUploadCommand(userId, result.objectKey(), uploadExpiresAt)));
		return new AiVideoUploadUrlResult(report.getId(), result.uploadUrl(), result.publicUrl(), result.objectKey(),
				result.expiresInSeconds());
	}
}
