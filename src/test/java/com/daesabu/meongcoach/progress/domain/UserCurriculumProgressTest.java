package com.daesabu.meongcoach.progress.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
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

	@Test
	@DisplayName("재진입하면 수정 시각이 갱신된다")
	void reenterRenewsUpdatedAt() {
		UserCurriculumProgress progress = UserCurriculumProgress.enter(1L, 3L);
		LocalDateTime beforeReenter = LocalDateTime.now();

		progress.reenter();

		assertThat(progress.getUpdatedAt()).isNotNull().isAfterOrEqualTo(beforeReenter);
	}
}
