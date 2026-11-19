package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LessonTest {

	@Test
	void 교육_순서는_카테고리_토픽_커리큘럼_레슨의_정렬_순서를_차례로_따른다() {
		TrainingCategory firstCategory = category(1L, 1);
		TrainingCategory secondCategory = category(2L, 2);
		Topic firstTopic = topic(firstCategory, 10L, 1);
		Topic secondTopic = topic(firstCategory, 11L, 2);
		Topic otherCategoryTopic = topic(secondCategory, 12L, 1);
		Curriculum firstCurriculum = curriculum(firstTopic, 100L, 1);
		Curriculum secondCurriculum = curriculum(firstTopic, 101L, 2);
		Lesson first = lesson(firstCurriculum, 1000L, 1);
		Lesson second = lesson(firstCurriculum, 1001L, 2);
		Lesson third = lesson(secondCurriculum, 1002L, 1);
		Lesson fourth = lesson(curriculum(secondTopic, 102L, 1), 1003L, 1);
		Lesson fifth = lesson(curriculum(otherCategoryTopic, 103L, 1), 1004L, 1);

		List<Lesson> ordered = Stream.of(fifth, third, first, fourth, second)
				.sorted(Lesson.trainingOrder())
				.toList();

		assertThat(ordered).containsExactly(first, second, third, fourth, fifth);
	}

	@Test
	void 정렬_순서가_같으면_ID가_작은_쪽이_앞선다() {
		Curriculum curriculum = curriculum(topic(category(1L, 1), 10L, 1), 100L, 1);
		Lesson later = lesson(curriculum, 1001L, 1);
		Lesson earlier = lesson(curriculum, 1000L, 1);

		List<Lesson> ordered = Stream.of(later, earlier)
				.sorted(Lesson.trainingOrder())
				.toList();

		assertThat(ordered).containsExactly(earlier, later);
	}

	private TrainingCategory category(Long id, int sortOrder) {
		TrainingCategory category = TrainingCategoryFixture.create("카테고리", sortOrder, null, null);
		ReflectionTestUtils.setField(category, "id", id);
		return category;
	}

	private Topic topic(TrainingCategory category, Long id, int sortOrder) {
		Topic topic = TopicFixture.create(category, "토픽", sortOrder, null, null, null);
		ReflectionTestUtils.setField(topic, "id", id);
		return topic;
	}

	private Curriculum curriculum(Topic topic, Long id, int sortOrder) {
		Curriculum curriculum = CurriculumFixture.create(topic, "커리큘럼", sortOrder, null, null);
		ReflectionTestUtils.setField(curriculum, "id", id);
		return curriculum;
	}

	private Lesson lesson(Curriculum curriculum, Long id, int sortOrder) {
		Lesson lesson = LessonFixture.create(curriculum, "레슨", sortOrder, 5);
		ReflectionTestUtils.setField(lesson, "id", id);
		return lesson;
	}
}
