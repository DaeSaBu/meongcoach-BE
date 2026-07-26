package com.daesabu.meongcoach.progress.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UserCurriculumProgress 도메인")
class UserCurriculumProgressTest {

	@Test
	@DisplayName("입장하면 사용자와 토픽 정보가 설정된다")
	void enterSetsUserAndTopic() {
		UserCurriculumProgress progress = UserCurriculumProgress.enter(1L, 3L);

		assertThat(progress.getUserId()).isEqualTo(1L);
		assertThat(progress.getTopicId()).isEqualTo(3L);
	}
}
