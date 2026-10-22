package com.daesabu.meongcoach.onboarding.application;

import com.daesabu.meongcoach.dog.application.provided.DogRegister;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingCompleteInfo;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingCompleter;
import com.daesabu.meongcoach.user.application.provided.UserProfileRegisterRequest;
import com.daesabu.meongcoach.user.application.provided.UserRegister;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OnboardingCompleteService implements OnboardingCompleter {

	private final UserRegister userRegister;
	private final DogRegister dogRegister;

	@Override
	@Transactional
	public List<Long> complete(Long userId, OnboardingCompleteInfo info) {
		userRegister.completeOnboarding(userId,
				new UserProfileRegisterRequest(info.nickname(), info.profileImageUrl(), info.mbti(),
						info.priorTrainingTopicIds(), info.trainingGoalTopicIds()));
		return info.dogs().stream()
				.map(dog -> dogRegister.register(userId, dog))
				.toList();
	}
}
