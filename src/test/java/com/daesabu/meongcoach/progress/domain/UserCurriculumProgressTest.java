package com.daesabu.meongcoach.progress.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserCurriculumProgressTest {

	@Test
	void enterSetsUserAndTopic() {
		UserCurriculumProgress progress = UserCurriculumProgress.enter(1L, 3L);

		assertThat(progress.getUserId()).isEqualTo(1L);
		assertThat(progress.getTopicId()).isEqualTo(3L);
	}
}
