package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import org.junit.jupiter.api.Test;

class LessonTest {

	@Test
	void 유료_커리큘럼의_레슨은_카테고리의_이용권을_요구한다() {
		Lesson lesson = lessonIn(EntitlementType.PUPPY, true);

		assertThat(lesson.findRequiredEntitlementType()).contains(EntitlementType.PUPPY);
	}

	@Test
	void 맛보기_커리큘럼의_레슨은_이용권을_요구하지_않는다() {
		Lesson lesson = lessonIn(EntitlementType.PUPPY, false);

		assertThat(lesson.findRequiredEntitlementType()).isEmpty();
	}

	private Lesson lessonIn(EntitlementType requiredEntitlementType, boolean isPremium) {
		TrainingCategory category = TrainingCategoryFixture.create("퍼피 교육", 1, null, null, requiredEntitlementType);
		Topic topic = TopicFixture.create(category, "앉아", 1, null, null, null);
		Curriculum curriculum = CurriculumFixture.create(topic, "커리큘럼", 1, null, null, isPremium);
		return LessonFixture.create(curriculum, "레슨", 1, 5);
	}
}
