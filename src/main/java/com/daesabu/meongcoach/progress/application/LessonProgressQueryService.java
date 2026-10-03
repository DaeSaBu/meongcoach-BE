package com.daesabu.meongcoach.progress.application;

import com.daesabu.meongcoach.progress.application.provided.LessonProgressFinder;
import com.daesabu.meongcoach.progress.application.required.LessonProgressRepository;
import com.daesabu.meongcoach.progress.domain.LessonProgress;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonProgressQueryService implements LessonProgressFinder {
	private static final int COMPLETED_THRESHOLD = 1;

	private final LessonProgressRepository lessonProgressRepository;

	@Override
	public Set<Long> findCompletedLessonIds(Long userId, Collection<Long> lessonIds) {
		return lessonProgressRepository
				.findAllByUserIdAndLessonIdInAndCompletedCountGreaterThanEqual(
						userId, lessonIds, COMPLETED_THRESHOLD)
				.stream()
				.map(LessonProgress::getLessonId)
				.collect(Collectors.toUnmodifiableSet());
	}

	@Override
	public Map<Long, Integer> findCompletedCounts(Long userId, Collection<Long> lessonIds) {
		Map<Long, Integer> recordedCounts = lessonProgressRepository
				.findAllByUserIdAndLessonIdIn(userId, lessonIds).stream()
				.collect(Collectors.toMap(LessonProgress::getLessonId, LessonProgress::getCompletedCount));

		Map<Long, Integer> completedCounts = new LinkedHashMap<>();
		for (Long lessonId : lessonIds) {
			completedCounts.put(lessonId, recordedCounts.getOrDefault(lessonId, 0));
		}
		return completedCounts;
	}
}
