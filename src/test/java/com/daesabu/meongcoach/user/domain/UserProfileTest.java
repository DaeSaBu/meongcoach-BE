package com.daesabu.meongcoach.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
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
	void changeBirthDateReplacesBirthDate() {
		UserProfile profile = UserProfile.create(User.registerMember(), "멍멍이집사");

		profile.changeBirthDate(LocalDate.of(2000, 1, 1));

		assertThat(profile.getBirthDate()).isEqualTo(LocalDate.of(2000, 1, 1));
	}

	@Test
	void changeMbtiReplacesMbti() {
		UserProfile profile = UserProfile.create(User.registerMember(), "멍멍이집사");

		profile.changeMbti(Mbti.INFP);

		assertThat(profile.getMbti()).isEqualTo(Mbti.INFP);
	}

	@Test
	void getAgeCalculatesFromBirthDate() {
		UserProfile profile = UserProfile.create(User.registerMember(), "멍멍이집사");
		profile.changeBirthDate(LocalDate.now().minusYears(20));

		assertThat(profile.getAge()).isEqualTo(20);
	}

	@Test
	void getAgeReturnsNullWhenBirthDateIsNull() {
		UserProfile profile = UserProfile.create(User.registerMember(), "멍멍이집사");

		assertThat(profile.getAge()).isNull();
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
