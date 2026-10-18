package com.daesabu.meongcoach.graduation.application;

import com.daesabu.meongcoach.graduation.application.provided.GraduationUploadUrlIssuer;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationUploadUrlRequest;
import com.daesabu.meongcoach.graduation.application.required.TopicGraduationRepository;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationAlreadyApprovedException;
import com.daesabu.meongcoach.graduation.domain.exception.TopicNotCompletedException;
import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlResult;
import com.daesabu.meongcoach.training.application.provided.TopicCompletionChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraduationUploadUrlService implements GraduationUploadUrlIssuer {

	private static final String UPLOAD_TARGET = "GRADUATION_VIDEO";

	private final TopicCompletionChecker topicCompletionChecker;
	private final TopicGraduationRepository topicGraduationRepository;
	private final VideoUploadUrlIssuer videoUploadUrlIssuer;

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
		// 실습 인증 영상은 토픽 단위로 올리므로 토픽 ID를 키의 묶음 단계(videos/graduation/{userId}/{topicId}/)에 넣는다
		return videoUploadUrlIssuer.issue(userId, UPLOAD_TARGET, topicId, request.contentType(), request.fileSizeBytes());
	}
}
