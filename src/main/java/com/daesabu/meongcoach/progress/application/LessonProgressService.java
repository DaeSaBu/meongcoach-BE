package com.daesabu.meongcoach.progress.application;

import com.daesabu.meongcoach.progress.application.provided.LessonProgressFinder;
import com.daesabu.meongcoach.progress.application.provided.LessonProgressRecorder;
import com.daesabu.meongcoach.progress.application.required.UserLessonProgressRepository;
import com.daesabu.meongcoach.progress.domain.UserLessonProgress;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 레슨 진행도 조회·기록 서비스. 모듈 공개 API 두 가지를 함께 구현한다.
 */
@Service
@RequiredArgsConstructor
public class LessonProgressService implements LessonProgressFinder, LessonProgressRecorder {

	private static final int COMPLETED_THRESHOLD = 1;

	private final UserLessonProgressRepository userLessonProgressRepository;

	@Override
	@Transactional(readOnly = true)
	public Set<Long> findCompletedLessonIds(Long userId, Collection<Long> lessonIds) {
		return userLessonProgressRepository.findAllByUserIdAndLessonIdIn(userId, lessonIds).stream()
				.filter(progress -> progress.getCompletedCount() >= COMPLETED_THRESHOLD)
				.map(UserLessonProgress::getLessonId)
				.collect(Collectors.toUnmodifiableSet());
	}

	@Override
	@Transactional(readOnly = true)
	public Map<Long, Integer> findCompletedCounts(Long userId, Collection<Long> lessonIds) {
		Map<Long, Integer> recordedCounts = userLessonProgressRepository
				.findAllByUserIdAndLessonIdIn(userId, lessonIds).stream()
				.collect(Collectors.toMap(UserLessonProgress::getLessonId, UserLessonProgress::getCompletedCount));

		Map<Long, Integer> completedCounts = new LinkedHashMap<>();
		for (Long lessonId : lessonIds) {
			completedCounts.put(lessonId, recordedCounts.getOrDefault(lessonId, 0));
		}
		return completedCounts;
	}

	@Override
	@Transactional
	public int completeLesson(Long userId, Long lessonId) {
		UserLessonProgress progress = userLessonProgressRepository.findByUserIdAndLessonId(userId, lessonId)
				.orElseGet(() -> userLessonProgressRepository.save(UserLessonProgress.start(userId, lessonId)));

		progress.increaseCompletedCount();
		return progress.getCompletedCount();
	}
}
