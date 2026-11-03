package com.daesabu.meongcoach.onboarding.application;

import com.daesabu.meongcoach.dog.application.provided.BreedFinder;
import com.daesabu.meongcoach.dog.application.provided.PersonalityFinder;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingMetadataFinder;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingMetadataResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OnboardingMetadataService implements OnboardingMetadataFinder {

	private final BreedFinder breedFinder;
	private final PersonalityFinder personalityFinder;

	@Override
	public OnboardingMetadataResult find() {
		var breeds = breedFinder.findAll();
		var personalities = personalityFinder.findAll();
		return new OnboardingMetadataResult(breeds, personalities);
	}
}
