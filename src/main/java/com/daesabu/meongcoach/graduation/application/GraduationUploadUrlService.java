package com.daesabu.meongcoach.graduation.application;

import com.daesabu.meongcoach.graduation.application.provided.GraduationUploadUrlIssuer;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationUploadUrlRequest;
import com.daesabu.meongcoach.graduation.application.required.TopicGraduationRepository;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationAlreadyApprovedException;
import com.daesabu.meongcoach.graduation.domain.exception.TopicNotCompletedException;
import com.daesabu.meongcoach.media.application.provided.GraduationVideoUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlResult;
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
	private final GraduationVideoUploadUrlIssuer graduationVideoUploadUrlIssuer;

	@Override
	public VideoUploadUrlResult issue(Long userId, Long topicId, GraduationUploadUrlRequest request) {
		if (!topicCompletionChecker.isCompleted(userId, topicId)) {
			throw new TopicNotCompletedException(topicId);
		}
		boolean approved = topicGraduationRepository.findByUserIdAndTopicId(userId, topicId)
				.filter(TopicGraduation::isApproved)
				.isPresent();
		if (approved) {
			throw new GraduationAlreadyApprovedException(topicId);
		}
		return graduationVideoUploadUrlIssuer.issue(userId, topicId, request.contentType(), request.fileSizeBytes());
	}
}
