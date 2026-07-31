package com.daesabu.meongcoach.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UserProfile 도메인")
class UserProfileTest {

	@Test
	@DisplayName("생성하면 툴팁 완료 여부가 false로 초기화된다")
	void createInitializesTooltipFlagToFalse() {
		UserProfile profile = UserProfile.create(User.registerMember(), "멍멍이집사");

		assertThat(profile.getNickname()).isEqualTo("멍멍이집사");
		assertThat(profile.getIsCompletedTooltip()).isFalse();
	}

	@Test
	@DisplayName("닉네임을 변경하면 기존 닉네임이 교체된다")
	void changeNicknameReplacesNickname() {
		UserProfile profile = UserProfile.create(User.registerMember(), "멍멍이집사");

		profile.changeNickname("새집사");

		assertThat(profile.getNickname()).isEqualTo("새집사");
	}

	@Test
	@DisplayName("프로필 이미지를 변경하면 이미지 URL이 교체된다")
	void changeProfileImageReplacesImageUrl() {
		UserProfile profile = UserProfile.create(User.registerMember(), "멍멍이집사");

		profile.changeProfileImage("https://cdn.meongcoach.com/profile.png");

		assertThat(profile.getProfileImageUrl()).isEqualTo("https://cdn.meongcoach.com/profile.png");
	}

	@Test
	@DisplayName("생년월일을 변경하면 기존 생년월일이 교체된다")
	void changeBirthDateReplacesBirthDate() {
		UserProfile profile = UserProfile.create(User.registerMember(), "멍멍이집사");

		profile.changeBirthDate(LocalDate.of(2000, 1, 1));

		assertThat(profile.getBirthDate()).isEqualTo(LocalDate.of(2000, 1, 1));
	}

	@Test
	@DisplayName("MBTI를 변경하면 기존 MBTI가 교체된다")
	void changeMbtiReplacesMbti() {
		UserProfile profile = UserProfile.create(User.registerMember(), "멍멍이집사");

		profile.changeMbti(Mbti.INFP);

		assertThat(profile.getMbti()).isEqualTo(Mbti.INFP);
	}

	@Test
	@DisplayName("성별을 변경하면 기존 성별이 교체된다")
	void changeGenderReplacesGender() {
		UserProfile profile = UserProfile.create(User.registerMember(), "멍멍이집사");

		profile.changeGender(Gender.FEMALE);

		assertThat(profile.getGender()).isEqualTo(Gender.FEMALE);
	}

	@Test
	@DisplayName("생년월일로 나이를 계산한다")
	void getAgeCalculatesFromBirthDate() {
		UserProfile profile = UserProfile.create(User.registerMember(), "멍멍이집사");
		profile.changeBirthDate(LocalDate.now().minusYears(20));

		assertThat(profile.getAge()).isEqualTo(20);
	}

	@Test
	@DisplayName("생년월일이 없으면 나이는 null을 반환한다")
	void getAgeReturnsNullWhenBirthDateIsNull() {
		UserProfile profile = UserProfile.create(User.registerMember(), "멍멍이집사");

		assertThat(profile.getAge()).isNull();
	}

	@Test
	@DisplayName("툴팁을 완료하면 완료 상태로 표시된다")
	void completeTooltipMarksTooltipCompleted() {
		UserProfile profile = UserProfile.create(User.registerMember(), "멍멍이집사");

		profile.completeTooltip();

		assertThat(profile.getIsCompletedTooltip()).isTrue();
	}
}
