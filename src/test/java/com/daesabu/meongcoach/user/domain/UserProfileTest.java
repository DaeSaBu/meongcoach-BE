package com.daesabu.meongcoach.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.user.domain.command.UserProfileCreateCommand;
import com.daesabu.meongcoach.user.domain.exception.InvalidGenderException;
import com.daesabu.meongcoach.user.domain.exception.InvalidMbtiException;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UserProfileTest {

	@Test
	void 생성하면_Command의_값이_한_번에_채워지고_툴팁_완료_여부는_false로_초기화된다() {
		UserProfile profile = UserProfile.create(User.registerOnboardingMember(),
				command(LocalDate.of(2000, 1, 1), Set.of(), Set.of()));

		assertThat(profile.getNickname()).isEqualTo("멍멍이집사");
		assertThat(profile.getProfileImageUrl()).isEqualTo("https://cdn.meongcoach.com/profile.png");
		assertThat(profile.getBirthDate()).isEqualTo(LocalDate.of(2000, 1, 1));
		assertThat(profile.getMbti()).isEqualTo(Mbti.INFP);
		assertThat(profile.getGender()).isEqualTo(Gender.FEMALE);
		assertThat(profile.getIsCompletedTooltip()).isFalse();
	}

	@Test
	void 생성하면_교육_이력과_목표_토픽이_중복_없이_저장된다() {
		UserProfile profile = UserProfile.create(User.registerOnboardingMember(),
				command(null, Set.of(1L, 2L), Set.of(2L, 3L)));

		assertThat(profile.getPriorTrainingTopicIds()).containsExactlyInAnyOrder(1L, 2L);
		assertThat(profile.getTrainingGoalTopicIds()).containsExactlyInAnyOrder(2L, 3L);
	}

	@Test
	void 교육_토픽이_null이면_빈_집합으로_저장된다() {
		UserProfile profile = UserProfile.create(User.registerOnboardingMember(), command(null, null, null));

		assertThat(profile.getPriorTrainingTopicIds()).isEmpty();
		assertThat(profile.getTrainingGoalTopicIds()).isEmpty();
	}

	@Test
	void 프로필_이미지가_null이면_빈_문자열로_저장된다() {
		UserProfile profile = UserProfile.create(User.registerOnboardingMember(),
				new UserProfileCreateCommand("멍멍이집사", null, null, "INFP", "FEMALE", Set.of(), Set.of()));

		assertThat(profile.getProfileImageUrl()).isEmpty();
	}

	@Test
	void 유효하지_않은_MBTI_문자열로는_생성할_수_없다() {
		assertThatThrownBy(() -> UserProfile.create(User.registerOnboardingMember(),
				new UserProfileCreateCommand("멍멍이집사", null, null, "XXXX", "FEMALE", Set.of(), Set.of())))
				.isInstanceOf(InvalidMbtiException.class);
	}

	@Test
	void 유효하지_않은_성별_문자열로는_생성할_수_없다() {
		assertThatThrownBy(() -> UserProfile.create(User.registerOnboardingMember(),
				new UserProfileCreateCommand("멍멍이집사", null, null, "INFP", "OTHER", Set.of(), Set.of())))
				.isInstanceOf(InvalidGenderException.class);
	}

	@Test
	void 생년월일로_나이를_계산한다() {
		UserProfile profile = UserProfile.create(User.registerOnboardingMember(),
				command(LocalDate.now().minusYears(20), Set.of(), Set.of()));

		assertThat(profile.getAge()).isEqualTo(20);
	}

	@Test
	void 생년월일이_없으면_나이는_null을_반환한다() {
		UserProfile profile = UserProfile.create(User.registerOnboardingMember(), command(null, Set.of(), Set.of()));

		assertThat(profile.getAge()).isNull();
	}

	@Test
	void 툴팁을_완료하면_완료_상태로_표시된다() {
		UserProfile profile = UserProfile.create(User.registerOnboardingMember(), command(null, Set.of(), Set.of()));

		profile.completeTooltip();

		assertThat(profile.getIsCompletedTooltip()).isTrue();
	}

	private static UserProfileCreateCommand command(LocalDate birthDate, Set<Long> priorTrainingTopicIds,
			Set<Long> trainingGoalTopicIds) {
		return new UserProfileCreateCommand("멍멍이집사", "https://cdn.meongcoach.com/profile.png", birthDate,
				"INFP", "FEMALE", priorTrainingTopicIds, trainingGoalTopicIds);
	}
}
