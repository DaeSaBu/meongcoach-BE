package com.daesabu.meongcoach.onboarding.application.provided;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.dog.domain.shared.Personality;
import com.daesabu.meongcoach.dog.domain.shared.RegisteredBreed;
import com.daesabu.meongcoach.support.ApplicationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class OnboardingMetadataFinderTest {

	@Autowired
	private OnboardingMetadataFinder onboardingMetadataFinder;

	@Test
	void 견종_성격_목록을_한_번에_모아_반환한다() {
		OnboardingMetadataResult result = onboardingMetadataFinder.find();

		assertThat(result.breeds()).containsExactly(RegisteredBreed.values());
		assertThat(result.personalities()).extracting(Personality::name)
				.containsExactly("TIMID", "LIVELY", "FRIENDLY", "CALM", "FEARFUL", "AFFECTIONATE",
						"INDEPENDENT", "PLAYFUL", "EXCITABLE", "STUBBORN");
	}
}
