package com.daesabu.meongcoach.progress.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UserSelectedTopic 도메인")
class UserSelectedTopicTest {

	@Test
	@DisplayName("진입하면 사용자와 토픽 정보가 설정된다")
	void enterSetsUserAndTopic() {
		UserSelectedTopic selectedTopic = UserSelectedTopic.enter(1L, 3L);

		assertThat(selectedTopic.getUserId()).isEqualTo(1L);
		assertThat(selectedTopic.getTopicId()).isEqualTo(3L);
	}

	@Test
	@DisplayName("다른 토픽으로 옮기면 선택 토픽이 바뀐다")
	void moveToChangesTopic() {
		UserSelectedTopic selectedTopic = UserSelectedTopic.enter(1L, 3L);

		selectedTopic.moveTo(5L);

		assertThat(selectedTopic.getTopicId()).isEqualTo(5L);
	}
}
