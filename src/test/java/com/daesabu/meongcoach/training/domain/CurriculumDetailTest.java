package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CurriculumDetailTest {

	@Test
	void createSetsDescriptionAndDifficulty() {
		TrainingCategory category = TrainingCategory.create("기본 훈련", 1);
		Topic topic = Topic.create(category, "산책 훈련", 1);
		Curriculum curriculum = Curriculum.create(topic, "리드줄 적응", 1, null);

		CurriculumDetail detail = CurriculumDetail.create(curriculum, "리드줄에 익숙해지는 훈련", Difficulty.EASY);

		assertThat(detail.getCurriculum()).isEqualTo(curriculum);
		assertThat(detail.getDescription()).isEqualTo("리드줄에 익숙해지는 훈련");
		assertThat(detail.getDifficulty()).isEqualTo(Difficulty.EASY);
	}
}
