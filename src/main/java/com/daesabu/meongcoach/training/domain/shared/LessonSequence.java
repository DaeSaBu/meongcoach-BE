package com.daesabu.meongcoach.training.domain.shared;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class LessonSequence {

	private final List<List<Long>> topicLessonIds;

	public LessonSequence(List<List<Long>> topicLessonIds) {
		this.topicLessonIds = topicLessonIds.stream()
				.map(List::copyOf)
				.toList();
	}

	public boolean canComplete(Long lessonId, Set<Long> completedLessonIds) {
		return topicLessonIds.stream()
				.map(lessonIds -> findFirstIncompleteLessonId(lessonIds, completedLessonIds))
				.flatMap(Optional::stream)
				.anyMatch(lessonId::equals);
	}

	public List<Long> lessonIds() {
		return topicLessonIds.stream()
				.flatMap(List::stream)
				.toList();
	}

	private Optional<Long> findFirstIncompleteLessonId(List<Long> lessonIds, Set<Long> completedLessonIds) {
		return lessonIds.stream()
				.filter(lessonId -> !completedLessonIds.contains(lessonId))
				.findFirst();
	}
}
