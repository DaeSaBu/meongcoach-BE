package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Topic 도메인")
class TopicTest {

	private final TrainingCategory category = TrainingCategory.create("기본 교육", 1, null, null);

	@Test
	@DisplayName("생성하면 설명과 상세 설명 및 아이콘 URL이 설정된다")
	void createSetsDescriptionDetailAndIconUrl() {
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
	@DisplayName("설명과 상세 설명 및 아이콘 URL이 없으면 빈 문자열로 설정한다")
	void createSetsEmptyStringsWhenDescriptionDetailAndIconUrlAreNull() {
		Topic topic = Topic.create(category, new TopicCreateCommand("앉아", 1, null, null, null));

		assertThat(topic.getDescription()).isEmpty();
		assertThat(topic.getDetail()).isEmpty();
		assertThat(topic.getIconUrl()).isEmpty();
	}
}
