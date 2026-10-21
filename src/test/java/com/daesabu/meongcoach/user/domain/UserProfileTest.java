package com.daesabu.meongcoach.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.user.domain.exception.InvalidMbtiException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UserProfileTest {

	@Test
	void 생성하면_Command의_값이_한_번에_채워지고_툴팁_완료_여부는_false로_초기화된다() {
		UserProfile profile = UserProfile.create(command(Set.of(), Set.of()));

		assertThat(profile.getNickname()).isEqualTo("멍멍이집사");
		assertThat(profile.getProfileImageUrl()).isEqualTo("https://cdn.meongcoach.com/profile.png");
		assertThat(profile.getMbti()).isEqualTo(Mbti.INFP);
		assertThat(profile.getIsCompletedTooltip()).isFalse();
	}

	@Test
	void 생성하면_교육_이력과_목표_토픽이_중복_없이_저장된다() {
		UserProfile profile = UserProfile.create(command(Set.of(1L, 2L), Set.of(2L, 3L)));

		assertThat(profile.getPriorTrainingTopicIds()).containsExactlyInAnyOrder(1L, 2L);
		assertThat(profile.getTrainingGoalTopicIds()).containsExactlyInAnyOrder(2L, 3L);
	}

	@Test
	void 교육_토픽이_null이면_빈_집합으로_저장된다() {
		UserProfile profile = UserProfile.create(command(null, null));

		assertThat(profile.getPriorTrainingTopicIds()).isEmpty();
		assertThat(profile.getTrainingGoalTopicIds()).isEmpty();
	}

	@Test
	void 프로필_이미지가_null이면_빈_문자열로_저장된다() {
		UserProfile profile = UserProfile.create(
				new UserProfileCreateCommand("멍멍이집사", null, "INFP", Set.of(), Set.of()));

		assertThat(profile.getProfileImageUrl()).isEmpty();
	}

	@Test
	void 유효하지_않은_MBTI_문자열로는_생성할_수_없다() {
		assertThatThrownBy(() -> UserProfile.create(
				new UserProfileCreateCommand("멍멍이집사", null, "XXXX", Set.of(), Set.of())))
				.isInstanceOf(InvalidMbtiException.class);
	}

	@Test
	void 툴팁을_완료하면_완료_상태로_표시된다() {
		UserProfile profile = UserProfile.create(command(Set.of(), Set.of()));

		profile.completeTooltip();

		assertThat(profile.getIsCompletedTooltip()).isTrue();
	}

	private static UserProfileCreateCommand command(Set<Long> priorTrainingTopicIds, Set<Long> trainingGoalTopicIds) {
		return new UserProfileCreateCommand("멍멍이집사", "https://cdn.meongcoach.com/profile.png", "INFP",
				priorTrainingTopicIds, trainingGoalTopicIds);
	}
}
