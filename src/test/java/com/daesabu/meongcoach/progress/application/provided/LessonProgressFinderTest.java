package com.daesabu.meongcoach.progress.application.provided;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.progress.application.required.LessonProgressRepository;
import com.daesabu.meongcoach.progress.domain.LessonProgress;
import com.daesabu.meongcoach.support.ApplicationTest;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class LessonProgressFinderTest {

	private static final Long USER_ID = 1L;

	private static final Long OTHER_USER_ID = 2L;

	@Autowired
	private LessonProgressFinder lessonProgressFinder;

	@Autowired
	private LessonProgressRepository lessonProgressRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 완료_횟수가_1_이상인_레슨의_id만_반환한다() {
		saveProgress(USER_ID, 10L, 1);
		saveProgress(USER_ID, 20L, 0);
		flushAndClear();

		Set<Long> completedLessonIds = lessonProgressFinder.findCompletedLessonIds(USER_ID, List.of(10L, 20L));

		assertThat(completedLessonIds).containsExactly(10L);
	}

	@Test
	void 다른_사용자가_완료한_레슨의_id는_반환하지_않는다() {
		saveProgress(OTHER_USER_ID, 10L, 3);
		flushAndClear();

		Set<Long> completedLessonIds = lessonProgressFinder.findCompletedLessonIds(USER_ID, List.of(10L));

		assertThat(completedLessonIds).isEmpty();
	}

	@Test
	void 진행_기록이_없는_레슨의_완료_횟수는_0으로_조회된다() {
		saveProgress(USER_ID, 10L, 2);
		flushAndClear();

		Map<Long, Integer> completedCounts = lessonProgressFinder.findCompletedCounts(USER_ID, List.of(10L, 20L));

		assertThat(completedCounts).containsExactlyInAnyOrderEntriesOf(Map.of(10L, 2, 20L, 0));
	}

	@Test
	void 다른_사용자의_완료_횟수는_조회되지_않는다() {
		saveProgress(OTHER_USER_ID, 10L, 3);
		flushAndClear();

		Map<Long, Integer> completedCounts = lessonProgressFinder.findCompletedCounts(USER_ID, List.of(10L));

		assertThat(completedCounts).containsExactlyInAnyOrderEntriesOf(Map.of(10L, 0));
	}

	private void saveProgress(Long userId, Long lessonId, int completedCount) {
		LessonProgress progress = LessonProgress.start(userId, lessonId);
		for (int i = 0; i < completedCount; i++) {
			progress.increaseCompletedCount();
		}
		lessonProgressRepository.save(progress);
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
