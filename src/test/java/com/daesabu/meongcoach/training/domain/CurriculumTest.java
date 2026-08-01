package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Curriculum 도메인")
class CurriculumTest {

	private Curriculum createCurriculum() {
		TrainingCategory category = TrainingCategory.create("기본 교육", 1, null, null);
		Topic topic = Topic.create(category, new TopicCreateCommand("산책 교육", 1, null, null, null));
		return Curriculum.create(topic, new CurriculumCreateCommand("리드줄 적응", 1,
				"https://cdn.meongcoach.com/thumb.png", "리드줄에 익숙해지는 교육"));
	}

	@Test
	@DisplayName("생성하면 커리큘럼 필드가 설정된다")
	void createSetsFields() {
		Curriculum curriculum = createCurriculum();

		assertThat(curriculum.getTitle()).isEqualTo("리드줄 적응");
		assertThat(curriculum.getSortOrder()).isEqualTo(1);
		assertThat(curriculum.getThumbnailUrl()).isEqualTo("https://cdn.meongcoach.com/thumb.png");
		assertThat(curriculum.getDescription()).isEqualTo("리드줄에 익숙해지는 교육");
	}

	@Test
	@DisplayName("생성하면 토픽·카테고리와 연결된다")
	void createConnectsTopicAndCategory() {
		Curriculum curriculum = createCurriculum();

		assertThat(curriculum.getTopic().getTitle()).isEqualTo("산책 교육");
		assertThat(curriculum.getTopic().getTrainingCategory().getTitle()).isEqualTo("기본 교육");
		assertThat(curriculum.getTopic().getSortOrder()).isEqualTo(1);
		assertThat(curriculum.getTopic().getTrainingCategory().getSortOrder()).isEqualTo(1);
	}
}
