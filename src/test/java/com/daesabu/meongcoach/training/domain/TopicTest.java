package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TopicTest {

	private final TrainingCategory category = TrainingCategory.create("기본 교육", 1, null, null);

	@Test
	void 생성하면_설명과_상세_설명_및_아이콘_URL이_설정된다() {
		Topic topic = Topic.create(category, new TopicCreateCommand(
				"앉아",
				1,
				"앉아 자세를 배우는 훈련",
				"차분히 앉는 방법을 익혀요",
				"https://example.com/sit.png"
		));

		assertThat(topic.getDescription()).isEqualTo("앉아 자세를 배우는 훈련");
		assertThat(topic.getDetail()).isEqualTo("차분히 앉는 방법을 익혀요");
		assertThat(topic.getIconUrl()).isEqualTo("https://example.com/sit.png");
	}

	@Test
	void 설명과_상세_설명_및_아이콘_URL이_없으면_빈_문자열로_설정한다() {
		Topic topic = Topic.create(category, new TopicCreateCommand("앉아", 1, null, null, null));

		assertThat(topic.getDescription()).isEmpty();
		assertThat(topic.getDetail()).isEmpty();
		assertThat(topic.getIconUrl()).isEmpty();
	}
}
