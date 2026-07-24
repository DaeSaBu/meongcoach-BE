package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CurriculumTest {

	private Curriculum createCurriculum() {
		TrainingCategory category = TrainingCategory.create("기본 훈련", 1);
		Topic topic = Topic.create(category, "산책 훈련", 1);
		return Curriculum.create(topic, "리드줄 적응", 1, "https://cdn.meongcoach.com/thumb.png",
				"리드줄에 익숙해지는 훈련");
	}

	@Test
	void createSetsFields() {
		Curriculum curriculum = createCurriculum();

		assertThat(curriculum.getTitle()).isEqualTo("리드줄 적응");
		assertThat(curriculum.getSortOrder()).isEqualTo(1);
		assertThat(curriculum.getThumbnailUrl()).isEqualTo("https://cdn.meongcoach.com/thumb.png");
		assertThat(curriculum.getDescription()).isEqualTo("리드줄에 익숙해지는 훈련");
	}

	@Test
	void createConnectsTopicAndCategory() {
		Curriculum curriculum = createCurriculum();

		assertThat(curriculum.getTopic().getTitle()).isEqualTo("산책 훈련");
		assertThat(curriculum.getTopic().getTrainingCategory().getTitle()).isEqualTo("기본 훈련");
		assertThat(curriculum.getTopic().getSortOrder()).isEqualTo(1);
		assertThat(curriculum.getTopic().getTrainingCategory().getSortOrder()).isEqualTo(1);
	}
}
