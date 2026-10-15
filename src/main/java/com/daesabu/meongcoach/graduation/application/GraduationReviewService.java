package com.daesabu.meongcoach.graduation.application;

import com.daesabu.meongcoach.graduation.application.provided.GraduationReviewer;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationReviewRequest;
import com.daesabu.meongcoach.graduation.application.required.GraduationSubmissionRepository;
import com.daesabu.meongcoach.graduation.application.required.TopicGraduationRepository;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.ReviewDecision;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import com.daesabu.meongcoach.graduation.domain.exception.SubmissionNotFoundException;
import com.daesabu.meongcoach.graduation.domain.exception.SubmissionNotPendingException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraduationReviewService implements GraduationReviewer {

	private final TopicGraduationRepository topicGraduationRepository;
	private final GraduationSubmissionRepository graduationSubmissionRepository;

	@Override
	@Transactional
	public GraduationSubmission review(Long submissionId, GraduationReviewRequest request) {
		ReviewDecision decision = ReviewDecision.from(request.result());
		Long graduationId = graduationSubmissionRepository.findGraduationIdById(submissionId)
				.orElseThrow(() -> new SubmissionNotFoundException(submissionId));
		TopicGraduation graduation = topicGraduationRepository.findForUpdateById(graduationId).orElseThrow();
		GraduationSubmission submission = graduationSubmissionRepository.findById(submissionId).orElseThrow();
		if (!submission.isPending()) {
			throw new SubmissionNotPendingException(submissionId);
		}

		Instant now = Instant.now();
		if (decision == ReviewDecision.APPROVED) {
			submission.approve(request.note(), now);
			graduation.approve(now);
			return submission;
		}
		submission.requestRevision(request.note(), now);
		graduation.requestRevision();
		return submission;
	}
}
