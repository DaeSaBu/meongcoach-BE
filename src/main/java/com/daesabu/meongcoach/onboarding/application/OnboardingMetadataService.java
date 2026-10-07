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

/**
 * 온보딩 화면에 필요한 목록 데이터를 각 모듈에서 모아 반환한다.
 */
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
