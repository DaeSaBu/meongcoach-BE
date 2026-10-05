package com.daesabu.meongcoach.progress.application.provided;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.progress.application.required.LessonProgressRepository;
import com.daesabu.meongcoach.progress.domain.LessonProgress;
import com.daesabu.meongcoach.support.ApplicationTest;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class LessonProgressUpdaterTest {

	private static final Long USER_ID = 1L;

	@Autowired
	private LessonProgressUpdater lessonProgressUpdater;

	@Autowired
	private LessonProgressRepository lessonProgressRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 첫_완료_시_진행_기록이_생성되고_완료_횟수_1을_반환한다() {
		int completedCount = lessonProgressUpdater.updateCompletion(USER_ID, 10L);

		flushAndClear();
		assertThat(completedCount).isEqualTo(1);
		assertThat(lessonProgressRepository.findByUserIdAndLessonId(USER_ID, 10L))
				.get().extracting(LessonProgress::getCompletedCount).isEqualTo(1);
	}

	@Test
	void 반복_완료_시_호출할_때마다_완료_횟수가_1씩_증가한다() {
		lessonProgressUpdater.updateCompletion(USER_ID, 10L);
		lessonProgressUpdater.updateCompletion(USER_ID, 10L);
		int completedCount = lessonProgressUpdater.updateCompletion(USER_ID, 10L);

		flushAndClear();
		assertThat(completedCount).isEqualTo(3);
		assertThat(lessonProgressRepository.findByUserIdAndLessonId(USER_ID, 10L))
				.get().extracting(LessonProgress::getCompletedCount).isEqualTo(3);
	}

	@Test
	void 반복_완료해도_진행_기록은_한_건만_유지된다() {
		lessonProgressUpdater.updateCompletion(USER_ID, 10L);
		lessonProgressUpdater.updateCompletion(USER_ID, 10L);

		flushAndClear();
		assertThat(lessonProgressRepository.findAllByUserIdAndLessonIdIn(USER_ID, List.of(10L))).hasSize(1);
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
