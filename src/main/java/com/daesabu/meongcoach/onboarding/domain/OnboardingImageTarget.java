package com.daesabu.meongcoach.onboarding.domain;

import com.daesabu.meongcoach.onboarding.domain.exception.InvalidOnboardingImageTargetException;
import java.util.Arrays;

public enum OnboardingImageTarget {
	USER_PROFILE,
	DOG_PROFILE,
	;

	public static OnboardingImageTarget from(String value) {
		return Arrays.stream(values())
				.filter(target -> target.name().equals(value))
				.findFirst()
				.orElseThrow(() -> new InvalidOnboardingImageTargetException(value));
	}
}
