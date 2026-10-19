package com.daesabu.meongcoach.graduation.application;

import com.daesabu.meongcoach.graduation.application.provided.GraduationSubmitter;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.application.required.GraduationSubmissionRepository;
import com.daesabu.meongcoach.graduation.application.required.TopicGraduationRepository;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.GraduationVideoKey;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationAlreadyApprovedException;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationSubmissionConflictException;
import com.daesabu.meongcoach.graduation.domain.exception.TopicNotCompletedException;
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

	@Override
	@Transactional
	public TopicGraduation submit(Long userId, Long topicId, GraduationSubmissionRequest request) {
		GraduationVideoKey videoKey = GraduationVideoKey.of(request.objectKey(), userId, topicId);
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

	private TopicGraduation resubmit(TopicGraduation graduation, GraduationVideoKey videoKey, Instant now) {
		if (graduation.isApproved()) {
			throw new GraduationAlreadyApprovedException(graduation.getTopicId());
		}
		graduation.resubmit(videoKey.value(), now);
		return graduation;
	}

	private TopicGraduation start(Long userId, Long topicId, GraduationVideoKey videoKey, Instant now) {
		try {
			return topicGraduationRepository.saveAndFlush(TopicGraduation.start(userId, topicId, videoKey.value(), now));
		} catch (DataIntegrityViolationException e) {
			throw new GraduationSubmissionConflictException(topicId, e);
		}
	}
}
