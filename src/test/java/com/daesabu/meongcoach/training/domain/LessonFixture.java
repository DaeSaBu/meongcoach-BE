package com.daesabu.meongcoach.training.domain;

import org.springframework.test.util.ReflectionTestUtils;

public final class LessonFixture {

	private LessonFixture() {
	}

	public static Lesson create(Curriculum curriculum, String title, int sortOrder, Integer estimatedMinutes) {
		Lesson lesson = new Lesson();
		ReflectionTestUtils.setField(lesson, "curriculum", curriculum);
		ReflectionTestUtils.setField(lesson, "title", title);
		ReflectionTestUtils.setField(lesson, "sortOrder", sortOrder);
		ReflectionTestUtils.setField(lesson, "estimatedMinutes", estimatedMinutes);
		return lesson;
	}
}
