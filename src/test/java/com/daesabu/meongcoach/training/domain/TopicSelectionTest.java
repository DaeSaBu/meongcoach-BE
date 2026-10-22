package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TopicSelectionTest {

	@Test
	void 진입하면_사용자와_토픽_정보가_설정된다() {
		TopicSelection selectedTopic = TopicSelection.enter(1L, 3L);

		assertThat(selectedTopic.getUserId()).isEqualTo(1L);
		assertThat(selectedTopic.getTopicId()).isEqualTo(3L);
	}

	@Test
	void 다른_토픽으로_옮기면_선택_토픽이_바뀐다() {
		TopicSelection selectedTopic = TopicSelection.enter(1L, 3L);

		selectedTopic.moveTo(5L);

		assertThat(selectedTopic.getTopicId()).isEqualTo(5L);
	}
}
