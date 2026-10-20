package com.daesabu.meongcoach.onboarding.application;

import com.daesabu.meongcoach.dog.application.provided.DogRegister;
import com.daesabu.meongcoach.media.application.provided.StoredMediaVerifier;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingCompleteInfo;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingCompleter;
import com.daesabu.meongcoach.onboarding.domain.OnboardingImageTarget;
import com.daesabu.meongcoach.user.application.provided.UserProfileRegister;
import com.daesabu.meongcoach.user.application.provided.UserProfileRegisterRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OnboardingCompleteService implements OnboardingCompleter {

	private final UserProfileRegister userProfileRegister;
	private final DogRegister dogRegister;
	private final StoredMediaVerifier storedMediaVerifier;

	@Override
	@Transactional
	public List<Long> complete(Long userId, OnboardingCompleteInfo info) {
		storedMediaVerifier.validateUrl(OnboardingImageTarget.USER_PROFILE.getMediaScope(), userId,
				info.profileImageUrl());
		userProfileRegister.register(userId,
				new UserProfileRegisterRequest(info.nickname(), info.profileImageUrl(), info.mbti(),
						info.priorTrainingTopicIds(), info.trainingGoalTopicIds()));
		return info.dogs().stream()
				.map(dog -> dogRegister.register(userId, dog))
				.toList();
	}
}
