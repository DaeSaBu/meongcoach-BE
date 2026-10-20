package com.daesabu.meongcoach.graduation.application;

import com.daesabu.meongcoach.graduation.application.provided.GraduationSubmitter;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.application.required.GraduationSubmissionRepository;
import com.daesabu.meongcoach.graduation.application.required.TopicGraduationRepository;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.GraduationVideo;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationAlreadyApprovedException;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationSubmissionConflictException;
import com.daesabu.meongcoach.graduation.domain.exception.InvalidGraduationVideoKeyException;
import com.daesabu.meongcoach.graduation.domain.exception.TopicNotCompletedException;
import com.daesabu.meongcoach.media.application.provided.StoredMediaVerifier;
import com.daesabu.meongcoach.training.application.provided.TopicCompletionChecker;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraduationSubmitService implements GraduationSubmitter {

	private final TopicCompletionChecker topicCompletionChecker;
	private final TopicGraduationRepository topicGraduationRepository;
	private final GraduationSubmissionRepository graduationSubmissionRepository;
	private final StoredMediaVerifier storedMediaVerifier;

	@Override
	@Transactional
	public TopicGraduation submit(Long userId, Long topicId, GraduationSubmissionRequest request) {
		String videoKey = request.objectKey();
		if (!storedMediaVerifier.isOwnedObjectKey(GraduationVideo.scopeOf(topicId), userId, videoKey)) {
			throw new InvalidGraduationVideoKeyException(videoKey);
		}
		if (!topicCompletionChecker.isCompleted(userId, topicId)) {
			throw new TopicNotCompletedException(topicId);
		}
		Instant now = Instant.now();
		TopicGraduation graduation = topicGraduationRepository.findByUserIdAndTopicId(userId, topicId)
				.map(existing -> resubmit(existing, videoKey, now))
				.orElseGet(() -> start(userId, topicId, videoKey, now));
		topicGraduationRepository.flush();
		graduationSubmissionRepository.save(GraduationSubmission.record(graduation));
		return graduation;
	}

	private TopicGraduation resubmit(TopicGraduation graduation, String videoKey, Instant now) {
		if (graduation.isApproved()) {
			throw new GraduationAlreadyApprovedException(graduation.getTopicId());
		}
		graduation.resubmit(videoKey, now);
		return graduation;
	}

	private TopicGraduation start(Long userId, Long topicId, String videoKey, Instant now) {
		try {
			return topicGraduationRepository.saveAndFlush(TopicGraduation.start(userId, topicId, videoKey, now));
		} catch (DataIntegrityViolationException e) {
			throw new GraduationSubmissionConflictException(topicId, e);
		}
	}
}
