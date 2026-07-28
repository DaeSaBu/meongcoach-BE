package com.daesabu.meongcoach.onboarding.application;

import com.daesabu.meongcoach.dog.application.provided.PersonalityFinder;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingMetadataFinder;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingMetadataResult;
import com.daesabu.meongcoach.training.application.provided.TopicFinder;
import com.daesabu.meongcoach.user.application.provided.MbtiFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 온보딩 화면에 필요한 목록 데이터를 각 모듈에서 모아 반환한다.
 */
@Service
@RequiredArgsConstructor
public class OnboardingMetadataService implements OnboardingMetadataFinder {

	private final TopicFinder topicFinder;
	private final PersonalityFinder personalityFinder;
	private final MbtiFinder mbtiFinder;

	@Override
	public OnboardingMetadataResult find() {
		return new OnboardingMetadataResult(
				topicFinder.findAllOrdered(),
				personalityFinder.findAll(),
				mbtiFinder.findAllCodes());
	}
}
