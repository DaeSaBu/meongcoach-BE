package com.daesabu.meongcoach.graduation.application;

import com.daesabu.meongcoach.entitlement.application.provided.EntitlementGranter;
import com.daesabu.meongcoach.graduation.application.provided.GraduationReviewer;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationReviewRequest;
import com.daesabu.meongcoach.graduation.application.required.TopicGraduationRepository;
import com.daesabu.meongcoach.graduation.domain.ReviewDecision;
import com.daesabu.meongcoach.graduation.domain.RewardPolicy;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationNotFoundException;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationNotPendingException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraduationReviewService implements GraduationReviewer {

	private final TopicGraduationRepository topicGraduationRepository;
	private final EntitlementGranter entitlementGranter;

	@Override
	@Transactional
	public TopicGraduation review(Long graduationId, GraduationReviewRequest request) {
		ReviewDecision decision = ReviewDecision.from(request.result());
		TopicGraduation graduation = topicGraduationRepository.findForUpdateById(graduationId)
				.orElseThrow(() -> new GraduationNotFoundException(graduationId));
		if (!graduation.isSubmitted()) {
			throw new GraduationNotPendingException(graduationId);
		}
		Instant now = Instant.now();
		if (decision == ReviewDecision.REVISION_REQUESTED) {
			graduation.requestRevision(request.note(), now);
			return graduation;
		}
		Long userId = graduation.getUserId();
		topicGraduationRepository.lockByUserId(userId);
		Instant previousExpiresAt = topicGraduationRepository.findLatestRewardExpiresAt(userId).orElse(null);
		Instant expiresAt = RewardPolicy.expiresAt(now, previousExpiresAt);
		entitlementGranter.grant(userId, expiresAt);
		graduation.approve(request.note(), now, expiresAt);
		return graduation;
	}
}
