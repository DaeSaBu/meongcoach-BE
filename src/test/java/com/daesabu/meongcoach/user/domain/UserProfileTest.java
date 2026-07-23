package com.daesabu.meongcoach.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserProfileTest {

	@Test
	void createInitializesCompletionFlagsToFalse() {
		UserProfile profile = UserProfile.create(User.registerMember(), "멍멍이집사");

		assertThat(profile.getNickname()).isEqualTo("멍멍이집사");
		assertThat(profile.getIsCompletedTooltip()).isFalse();
		assertThat(profile.getIsCompletedOnboarding()).isFalse();
	}

	@Test
	void changeNicknameReplacesNickname() {
		UserProfile profile = UserProfile.create(User.registerMember(), "멍멍이집사");

		profile.changeNickname("새집사");

		assertThat(profile.getNickname()).isEqualTo("새집사");
	}

	@Test
	void changeProfileImageReplacesImageUrl() {
		UserProfile profile = UserProfile.create(User.registerMember(), "멍멍이집사");

		profile.changeProfileImage("https://cdn.meongcoach.com/profile.png");

		assertThat(profile.getProfileImageUrl()).isEqualTo("https://cdn.meongcoach.com/profile.png");
	}

	@Test
	void completeTooltipMarksTooltipCompleted() {
		UserProfile profile = UserProfile.create(User.registerMember(), "멍멍이집사");

		profile.completeTooltip();

		assertThat(profile.getIsCompletedTooltip()).isTrue();
	}

	@Test
	void completeOnboardingMarksOnboardingCompleted() {
		UserProfile profile = UserProfile.create(User.registerMember(), "멍멍이집사");

		profile.completeOnboarding();

		assertThat(profile.getIsCompletedOnboarding()).isTrue();
	}
}
