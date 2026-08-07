package com.daesabu.meongcoach.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.user.domain.command.UserProfileCreateCommand;
import com.daesabu.meongcoach.user.domain.exception.InvalidGenderException;
import com.daesabu.meongcoach.user.domain.exception.InvalidMbtiException;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UserProfile 도메인")
class UserProfileTest {

	@Test
	@DisplayName("생성하면 Command의 값이 한 번에 채워지고 툴팁 완료 여부는 false로 초기화된다")
	void createAssignsAllValuesFromCommand() {
		UserProfile profile = UserProfile.create(User.registerMember(),
				command(LocalDate.of(2000, 1, 1), Set.of(), Set.of()));

		assertThat(profile.getNickname()).isEqualTo("멍멍이집사");
		assertThat(profile.getProfileImageUrl()).isEqualTo("https://cdn.meongcoach.com/profile.png");
		assertThat(profile.getBirthDate()).isEqualTo(LocalDate.of(2000, 1, 1));
		assertThat(profile.getMbti()).isEqualTo(Mbti.INFP);
		assertThat(profile.getGender()).isEqualTo(Gender.FEMALE);
		assertThat(profile.getIsCompletedTooltip()).isFalse();
	}

	@Test
	@DisplayName("생성하면 교육 이력과 목표 토픽이 중복 없이 저장된다")
	void createStoresTrainingTopicsWithoutDuplicates() {
		UserProfile profile = UserProfile.create(User.registerMember(),
				command(null, Set.of(1L, 2L), Set.of(2L, 3L)));

		assertThat(profile.getPriorTrainingTopicIds()).containsExactlyInAnyOrder(1L, 2L);
		assertThat(profile.getTrainingGoalTopicIds()).containsExactlyInAnyOrder(2L, 3L);
	}

	@Test
	@DisplayName("교육 토픽이 null이면 빈 집합으로 저장된다")
	void createTreatsNullTrainingTopicsAsEmpty() {
		UserProfile profile = UserProfile.create(User.registerMember(), command(null, null, null));

		assertThat(profile.getPriorTrainingTopicIds()).isEmpty();
		assertThat(profile.getTrainingGoalTopicIds()).isEmpty();
	}

	@Test
	@DisplayName("프로필 이미지가 null이면 빈 문자열로 저장된다")
	void createDefaultsProfileImageUrlToEmptyWhenNull() {
		UserProfile profile = UserProfile.create(User.registerMember(),
				new UserProfileCreateCommand("멍멍이집사", null, null, "INFP", "FEMALE", Set.of(), Set.of()));

		assertThat(profile.getProfileImageUrl()).isEmpty();
	}

	@Test
	@DisplayName("유효하지 않은 MBTI 문자열로는 생성할 수 없다")
	void createFailsWhenMbtiIsInvalid() {
		assertThatThrownBy(() -> UserProfile.create(User.registerMember(),
				new UserProfileCreateCommand("멍멍이집사", null, null, "XXXX", "FEMALE", Set.of(), Set.of())))
				.isInstanceOf(InvalidMbtiException.class);
	}

	@Test
	@DisplayName("유효하지 않은 성별 문자열로는 생성할 수 없다")
	void createFailsWhenGenderIsInvalid() {
		assertThatThrownBy(() -> UserProfile.create(User.registerMember(),
				new UserProfileCreateCommand("멍멍이집사", null, null, "INFP", "OTHER", Set.of(), Set.of())))
				.isInstanceOf(InvalidGenderException.class);
	}

	@Test
	@DisplayName("생년월일로 나이를 계산한다")
	void getAgeCalculatesFromBirthDate() {
		UserProfile profile = UserProfile.create(User.registerMember(),
				command(LocalDate.now().minusYears(20), Set.of(), Set.of()));

		assertThat(profile.getAge()).isEqualTo(20);
	}

	@Test
	@DisplayName("생년월일이 없으면 나이는 null을 반환한다")
	void getAgeReturnsNullWhenBirthDateIsNull() {
		UserProfile profile = UserProfile.create(User.registerMember(), command(null, Set.of(), Set.of()));

		assertThat(profile.getAge()).isNull();
	}

	@Test
	@DisplayName("툴팁을 완료하면 완료 상태로 표시된다")
	void completeTooltipMarksTooltipCompleted() {
		UserProfile profile = UserProfile.create(User.registerMember(), command(null, Set.of(), Set.of()));

		profile.completeTooltip();

		assertThat(profile.getIsCompletedTooltip()).isTrue();
	}

	private static UserProfileCreateCommand command(LocalDate birthDate, Set<Long> priorTrainingTopicIds,
			Set<Long> trainingGoalTopicIds) {
		return new UserProfileCreateCommand("멍멍이집사", "https://cdn.meongcoach.com/profile.png", birthDate,
				"INFP", "FEMALE", priorTrainingTopicIds, trainingGoalTopicIds);
	}
}
