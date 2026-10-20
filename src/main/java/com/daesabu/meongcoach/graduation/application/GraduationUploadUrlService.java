package com.daesabu.meongcoach.graduation.application;

import com.daesabu.meongcoach.graduation.application.provided.GraduationUploadUrlIssuer;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationUploadUrlRequest;
import com.daesabu.meongcoach.graduation.application.required.TopicGraduationRepository;
import com.daesabu.meongcoach.graduation.domain.GraduationVideo;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationAlreadyApprovedException;
import com.daesabu.meongcoach.graduation.domain.exception.TopicNotCompletedException;
import com.daesabu.meongcoach.media.application.provided.MediaUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadRequest;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;
import com.daesabu.meongcoach.training.application.provided.TopicCompletionChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraduationUploadUrlService implements GraduationUploadUrlIssuer {

	private final TopicCompletionChecker topicCompletionChecker;
	private final TopicGraduationRepository topicGraduationRepository;
	private final MediaUploadUrlIssuer mediaUploadUrlIssuer;

	@Override
	public MediaUploadUrlResult issue(Long userId, Long topicId, GraduationUploadUrlRequest request) {
		if (!topicCompletionChecker.isCompleted(userId, topicId)) {
			throw new TopicNotCompletedException(topicId);
		}
		topicGraduationRepository.findByUserIdAndTopicId(userId, topicId)
				.filter(TopicGraduation::isApproved)
				.ifPresent(graduation -> {
					throw new GraduationAlreadyApprovedException(topicId);
				});
		MediaUploadRequest uploadRequest = MediaUploadRequest.video(GraduationVideo.scopeOf(topicId), userId,
				request.contentType(), request.fileSizeBytes(), GraduationVideo.MAX_FILE_SIZE_BYTES);
		return mediaUploadUrlIssuer.issue(uploadRequest);
	}
}
