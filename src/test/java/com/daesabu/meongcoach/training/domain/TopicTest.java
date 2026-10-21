package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.util.List;
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
	@Test
	void 전체_콘텐츠의_모든_커리큘럼을_완료해야_토픽이_완료된다() {
		Topic topic = TopicFixture.create(10L);
		Curriculum first = CurriculumFixture.create(topic, 100L);
		Curriculum second = CurriculumFixture.create(topic, 200L);
		Lesson firstLesson = LessonFixture.create(first, 1001L);
		Lesson secondLesson = LessonFixture.create(first, 1002L);
		Lesson thirdLesson = LessonFixture.create(second, 2001L);
		LessonProgresses progresses = progresses(firstLesson, secondLesson);

		assertThat(topic.isCompleted(progresses)).isFalse();
		progresses = progresses(firstLesson, secondLesson, thirdLesson);
		assertThat(topic.isCompleted(progresses)).isTrue();
	}

	@Test
	void 빈_토픽이나_빈_커리큘럼은_완료가_아니다() {
		Topic topic = TopicFixture.create(10L);
		LessonProgresses progresses = LessonProgresses.from(List.of());

		assertThat(topic.isCompleted(progresses)).isFalse();
		Curriculum curriculum = CurriculumFixture.create(topic, 100L);
		Lesson lesson = LessonFixture.create(curriculum, 1001L);
		progresses = progresses(lesson);
		curriculum.getLessons().clear();
		assertThat(topic.isCompleted(progresses)).isFalse();
		topic.getCurriculums().clear();
		assertThat(topic.isCompleted(progresses)).isFalse();
	}

	@Test
	void 기록된_레슨이_완료되어도_새로_추가된_레슨이_있으면_미완료다() {
		Topic topic = TopicFixture.create(10L);
		Curriculum curriculum = CurriculumFixture.create(topic, 100L);
		Lesson lesson = LessonFixture.create(curriculum, 1001L);
		LessonProgresses progresses = progresses(lesson);

		assertThat(topic.isCompleted(progresses)).isTrue();
		LessonFixture.create(curriculum, 1002L);
		assertThat(topic.isCompleted(progresses)).isFalse();
	}

	private LessonProgresses progresses(Lesson... lessons) {
		List<LessonProgress> records = List.of(lessons).stream().map(lesson -> {
			LessonProgress progress = LessonProgress.start(1L, lesson.getId());
			progress.complete();
			return progress;
		}).toList();
		return LessonProgresses.from(records);
	}
}
