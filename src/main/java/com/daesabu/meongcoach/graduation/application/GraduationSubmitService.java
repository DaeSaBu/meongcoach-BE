package com.daesabu.meongcoach.graduation.application;

import com.daesabu.meongcoach.graduation.application.provided.GraduationSubmitter;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.application.required.GraduationSubmissionRepository;
import com.daesabu.meongcoach.graduation.application.required.SubmissionNotice;
import com.daesabu.meongcoach.graduation.application.required.SubmissionNotifier;
import com.daesabu.meongcoach.graduation.application.required.TopicGraduationRepository;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmissionCreateCommand;
import com.daesabu.meongcoach.graduation.domain.GraduationVideoKey;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationAlreadyApprovedException;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationSubmissionConflictException;
import com.daesabu.meongcoach.graduation.domain.exception.TopicNotCompletedException;
import com.daesabu.meongcoach.training.application.provided.TopicCompletionChecker;
import com.daesabu.meongcoach.training.application.provided.TopicFinder;
import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraduationSubmitService implements GraduationSubmitter {

	private final TopicCompletionChecker topicCompletionChecker;
	private final TopicFinder topicFinder;
	private final TopicGraduationRepository topicGraduationRepository;
	private final GraduationSubmissionRepository graduationSubmissionRepository;
	private final SubmissionNotifier submissionNotifier;

	@Override
	@Transactional
	public GraduationSubmission submit(Long userId, Long topicId, GraduationSubmissionRequest request) {
		GraduationVideoKey videoKey = GraduationVideoKey.of(request.objectKey(), userId);
		if (!topicCompletionChecker.isCompleted(userId, topicId)) {
			throw new TopicNotCompletedException(topicId);
		}
		TopicGraduation graduation = topicGraduationRepository.findForUpdate(userId, topicId)
				.orElseGet(() -> start(userId, topicId));
		if (graduation.isApproved()) {
			throw new GraduationAlreadyApprovedException(topicId);
		}
		graduation.resubmit();

		Optional<GraduationSubmission> latest =
				graduationSubmissionRepository.findTopByGraduationIdOrderByAttemptNoDesc(graduation.getId());
		latest.filter(GraduationSubmission::isPending).ifPresent(GraduationSubmission::replace);
		int attemptNo = latest.map(previous -> previous.getAttemptNo() + 1).orElse(1);

		GraduationSubmission submission = graduationSubmissionRepository.saveAndFlush(GraduationSubmission.create(
				graduation, new GraduationSubmissionCreateCommand(attemptNo, videoKey.value(), Instant.now())));
		notifySubmitted(submission, userId, topicId);
		return submission;
	}

	private TopicGraduation start(Long userId, Long topicId) {
		try {
			return topicGraduationRepository.saveAndFlush(TopicGraduation.start(userId, topicId));
		} catch (DataIntegrityViolationException e) {
			throw new GraduationSubmissionConflictException(topicId, e);
		}
	}

	private void notifySubmitted(GraduationSubmission submission, Long userId, Long topicId) {
		TopicResult topic = topicFinder.findTopic(topicId);
		submissionNotifier.notifySubmitted(new SubmissionNotice(submission.getId(), userId, topicId, topic.title(),
				submission.getAttemptNo(), submission.getSubmittedAt()));
	}
}
