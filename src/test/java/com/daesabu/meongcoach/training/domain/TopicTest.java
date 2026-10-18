package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TopicTest {

	@Test
	void 카테고리가_요구하는_이용권을_반환한다() {
		Topic topic = topicIn(EntitlementType.PUPPY);

		assertThat(topic.findRequiredEntitlementType()).contains(EntitlementType.PUPPY);
	}

	@Test
	void 카테고리가_이용권을_요구하지_않으면_빈_값을_반환한다() {
		Topic topic = topicIn(null);

		assertThat(topic.findRequiredEntitlementType()).isEmpty();
	}

	@Test
	void 커리큘럼이_없는_토픽은_완료가_아니다() {
		Topic topic = topicIn(null);

		assertThat(topic.isCompleted(Set.of())).isFalse();
	}

	@Test
	void 모든_커리큘럼의_레슨을_끝냈을_때만_완료다() {
		Topic topic = topicIn(null);
		Curriculum first = CurriculumFixture.create(topic, "1단계", 1, null, null);
		Curriculum second = CurriculumFixture.create(topic, "2단계", 2, null, null);
		attachLesson(first, 1L);
		attachLesson(second, 2L);
		ReflectionTestUtils.setField(topic, "curriculums", List.of(first, second));

		assertThat(topic.isCompleted(Set.of(1L))).isFalse();
		assertThat(topic.isCompleted(Set.of(1L, 2L))).isTrue();
	}

	@Test
	void 모든_커리큘럼의_레슨_ID를_모아_반환한다() {
		Topic topic = topicIn(null);
		Curriculum first = CurriculumFixture.create(topic, "1단계", 1, null, null);
		Curriculum second = CurriculumFixture.create(topic, "2단계", 2, null, null);
		attachLesson(first, 1L);
		attachLesson(second, 2L);
		ReflectionTestUtils.setField(topic, "curriculums", List.of(first, second));

		assertThat(topic.getAllLessonIds()).containsExactly(1L, 2L);
	}

	private void attachLesson(Curriculum curriculum, Long lessonId) {
		Lesson lesson = LessonFixture.create(curriculum, "레슨", 1, 5);
		ReflectionTestUtils.setField(lesson, "id", lessonId);
		ReflectionTestUtils.setField(curriculum, "lessons", List.of(lesson));
	}

	private Topic topicIn(EntitlementType requiredEntitlementType) {
		TrainingCategory category = TrainingCategoryFixture.create("퍼피 교육", 1, null, null, requiredEntitlementType);
		return TopicFixture.create(category, "앉아", 1, null, null, null);
	}
}
