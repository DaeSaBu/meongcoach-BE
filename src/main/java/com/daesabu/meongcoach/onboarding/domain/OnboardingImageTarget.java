package com.daesabu.meongcoach.onboarding.domain;

import com.daesabu.meongcoach.dog.domain.shared.DogProfileImage;
import com.daesabu.meongcoach.media.domain.shared.MediaKind;
import com.daesabu.meongcoach.media.domain.shared.MediaScope;
import com.daesabu.meongcoach.media.domain.shared.MediaVisibility;
import com.daesabu.meongcoach.onboarding.domain.exception.InvalidOnboardingImageTargetException;
import java.util.Arrays;
import lombok.Getter;

@Getter
public enum OnboardingImageTarget {
	USER_PROFILE(MediaScope.of(MediaKind.IMAGE, MediaVisibility.PUBLIC, "user-profile")),
	DOG_PROFILE(DogProfileImage.SCOPE),
	;

	private final MediaScope mediaScope;

	OnboardingImageTarget(MediaScope mediaScope) {
		this.mediaScope = mediaScope;
	}

	public static OnboardingImageTarget from(String value) {
		return Arrays.stream(values())
				.filter(target -> target.name().equals(value))
				.findFirst()
				.orElseThrow(() -> new InvalidOnboardingImageTargetException(value));
	}
}
