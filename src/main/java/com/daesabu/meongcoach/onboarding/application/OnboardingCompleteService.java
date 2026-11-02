package com.daesabu.meongcoach.onboarding.application;

import com.daesabu.meongcoach.dog.application.provided.DogRegister;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingCompleter;
import com.daesabu.meongcoach.onboarding.application.provided.dto.OnboardingCompleteRequest;
import com.daesabu.meongcoach.user.application.provided.UserRegister;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class OnboardingCompleteService implements OnboardingCompleter {

	private final UserRegister userRegister;
	private final DogRegister dogRegister;

	@Override
	@Transactional
	public List<Long> complete(Long userId, OnboardingCompleteRequest request) {
		userRegister.completeOnboarding(userId);
		return request.dogs().stream()
				.map(dog -> dogRegister.register(userId, dog.toInfo()))
				.toList();
	}
}
