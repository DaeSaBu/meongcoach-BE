package com.daesabu.meongcoach.training.domain;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LessonProgresses {

	private final Map<Long, LessonProgress> progresses;

	private LessonProgresses(Map<Long, LessonProgress> progresses) {
		this.progresses = progresses;
	}

	public static LessonProgresses from(Collection<LessonProgress> progresses) {
		Map<Long, LessonProgress> byLessonId = progresses.stream().collect(Collectors.toUnmodifiableMap(
				LessonProgress::getLessonId, Function.identity()));
		return new LessonProgresses(byLessonId);
	}

	public int countCompletedIn(Collection<Long> lessonIds) {
		return (int) lessonIds.stream()
				.map(progresses::get)
				.filter(Objects::nonNull)
				.filter(LessonProgress::isCompleted)
				.count();
	}

	public int completedCount(Long lessonId) {
		return Optional.ofNullable(progresses.get(lessonId))
				.map(LessonProgress::getCompletedCount)
				.orElse(0);
	}
}
