package com.daesabu.meongcoach.training.domain;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
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
		return (int) lessonIds.stream().map(progresses::get).filter(Objects::nonNull)
				.filter(progress -> progress.status() == ProgressStatus.COMPLETED).count();
	}

	public Map<Long, Integer> completedCounts(Collection<Long> lessonIds) {
		Map<Long, Integer> counts = new LinkedHashMap<>();
		for (Long lessonId : lessonIds) {
			LessonProgress progress = progresses.get(lessonId);
			int count = 0;
			if (progress != null) {
				count = progress.getCompletedCount();
			}
			counts.put(lessonId, count);
		}
		return counts;
	}
}
