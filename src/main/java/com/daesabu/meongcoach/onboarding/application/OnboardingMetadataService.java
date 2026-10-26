package com.daesabu.meongcoach.onboarding.application;

import com.daesabu.meongcoach.dog.application.provided.BreedFinder;
import com.daesabu.meongcoach.dog.application.provided.PersonalityFinder;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingMetadataFinder;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingMetadataResult;
import com.daesabu.meongcoach.training.application.provided.TopicFinder;
import com.daesabu.meongcoach.user.application.provided.MbtiFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OnboardingMetadataService implements OnboardingMetadataFinder {

	private final TopicFinder topicFinder;
	private final BreedFinder breedFinder;
	private final PersonalityFinder personalityFinder;
	private final MbtiFinder mbtiFinder;

	@Override
	public OnboardingMetadataResult find() {
		return new OnboardingMetadataResult(
				topicFinder.findAllOrdered(),
				breedFinder.findAll(),
				personalityFinder.findAll(),
				mbtiFinder.findAllCodes());
	}
}
