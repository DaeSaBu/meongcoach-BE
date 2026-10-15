package com.daesabu.meongcoach.graduation.application;

import com.daesabu.meongcoach.entitlement.application.provided.EntitlementGranter;
import com.daesabu.meongcoach.graduation.application.provided.GraduationRewardClaimer;
import com.daesabu.meongcoach.graduation.application.required.TopicGraduationRepository;
import com.daesabu.meongcoach.graduation.domain.RewardPolicy;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationNotApprovedException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraduationRewardService implements GraduationRewardClaimer {

	private final TopicGraduationRepository topicGraduationRepository;
	private final EntitlementGranter entitlementGranter;

	@Override
	@Transactional
	public TopicGraduation claim(Long userId, Long topicId) {
		topicGraduationRepository.lockByUserId(userId);
		TopicGraduation graduation = topicGraduationRepository.findByUserIdAndTopicId(userId, topicId)
				.filter(TopicGraduation::isApproved)
				.orElseThrow(() -> new GraduationNotApprovedException(topicId));
		if (graduation.isRewardClaimed()) {
			return graduation;
		}

		Instant now = Instant.now();
		Instant previousExpiresAt = topicGraduationRepository.findLatestRewardExpiresAt(userId).orElse(null);
		Instant expiresAt = RewardPolicy.expiresAt(now, previousExpiresAt);
		entitlementGranter.grant(userId, expiresAt);
		graduation.claimReward(now, expiresAt);
		return graduation;
	}
}
