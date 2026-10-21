package com.daesabu.meongcoach.training.application.provided;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.application.required.LessonProgressRepository;
import com.daesabu.meongcoach.training.domain.LessonProgress;
import com.daesabu.meongcoach.training.domain.LessonProgresses;
import jakarta.persistence.EntityManager;
import java.util.List;
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
	void 진행_기록이_없는_레슨의_완료_횟수는_0으로_조회된다() {
		saveProgress(USER_ID, 10L, 2);
		flushAndClear();

		LessonProgresses progresses = lessonProgressFinder.findProgresses(USER_ID, List.of(10L, 20L));

		assertThat(progresses.completedCount(10L)).isEqualTo(2);
		assertThat(progresses.completedCount(20L)).isZero();
	}

	@Test
	void 다른_사용자의_완료_횟수는_조회되지_않는다() {
		saveProgress(OTHER_USER_ID, 10L, 3);
		flushAndClear();

		LessonProgresses progresses = lessonProgressFinder.findProgresses(USER_ID, List.of(10L));

		assertThat(progresses.completedCount(10L)).isZero();
	}

	private void saveProgress(Long userId, Long lessonId, int completedCount) {
		LessonProgress progress = LessonProgress.start(userId, lessonId);
		for (int i = 0; i < completedCount; i++) {
			progress.complete();
		}
		lessonProgressRepository.save(progress);
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
