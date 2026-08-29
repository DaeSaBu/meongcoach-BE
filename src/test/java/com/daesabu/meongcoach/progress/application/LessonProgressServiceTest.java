package com.daesabu.meongcoach.progress.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.progress.application.required.UserLessonProgressRepository;
import com.daesabu.meongcoach.progress.domain.UserLessonProgress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * 레슨 진행도 모듈 공개 API 검증.
 */
@DataJpaTest
@Import(LessonProgressService.class)
class LessonProgressServiceTest {

	private static final Long USER_ID = 1L;

	private static final Long OTHER_USER_ID = 2L;

	@Autowired
	private LessonProgressService lessonProgressService;

	@Autowired
	private UserLessonProgressRepository userLessonProgressRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 완료_횟수가_1_이상인_레슨의_id만_반환한다() {
		persistProgress(USER_ID, 10L, 1);
		persistProgress(USER_ID, 20L, 0);
		entityManager.flush();

		Set<Long> completedLessonIds = lessonProgressService.findCompletedLessonIds(USER_ID, List.of(10L, 20L));

		assertThat(completedLessonIds).containsExactly(10L);
	}

	@Test
	void 다른_사용자가_완료한_레슨의_id는_반환하지_않는다() {
		persistProgress(OTHER_USER_ID, 10L, 3);
		entityManager.flush();

		Set<Long> completedLessonIds = lessonProgressService.findCompletedLessonIds(USER_ID, List.of(10L));

		assertThat(completedLessonIds).isEmpty();
	}

	@Test
	void 진행_기록이_없는_레슨의_완료_횟수는_0으로_조회된다() {
		persistProgress(USER_ID, 10L, 2);
		entityManager.flush();

		Map<Long, Integer> completedCounts = lessonProgressService.findCompletedCounts(USER_ID, List.of(10L, 20L));

		assertThat(completedCounts).containsExactlyInAnyOrderEntriesOf(Map.of(10L, 2, 20L, 0));
	}

	@Test
	void 다른_사용자의_완료_횟수는_조회되지_않는다() {
		persistProgress(OTHER_USER_ID, 10L, 3);
		entityManager.flush();

		Map<Long, Integer> completedCounts = lessonProgressService.findCompletedCounts(USER_ID, List.of(10L));

		assertThat(completedCounts).containsExactlyInAnyOrderEntriesOf(Map.of(10L, 0));
	}

	@Test
	void 첫_완료_시_진행_기록이_생성되고_완료_횟수_1을_반환한다() {
		int completedCount = lessonProgressService.completeLesson(USER_ID, 10L);

		entityManager.flush();
		entityManager.clear();
		assertThat(completedCount).isEqualTo(1);
		assertThat(userLessonProgressRepository.findByUserIdAndLessonId(USER_ID, 10L))
				.get().extracting(UserLessonProgress::getCompletedCount).isEqualTo(1);
	}

	@Test
	void 반복_완료_시_호출할_때마다_완료_횟수가_1씩_증가한다() {
		lessonProgressService.completeLesson(USER_ID, 10L);
		lessonProgressService.completeLesson(USER_ID, 10L);
		int completedCount = lessonProgressService.completeLesson(USER_ID, 10L);

		entityManager.flush();
		entityManager.clear();
		assertThat(completedCount).isEqualTo(3);
		assertThat(userLessonProgressRepository.findByUserIdAndLessonId(USER_ID, 10L))
				.get().extracting(UserLessonProgress::getCompletedCount).isEqualTo(3);
	}

	@Test
	void 반복_완료해도_진행_기록은_한_건만_유지된다() {
		lessonProgressService.completeLesson(USER_ID, 10L);
		lessonProgressService.completeLesson(USER_ID, 10L);

		entityManager.flush();
		entityManager.clear();
		assertThat(userLessonProgressRepository.findAllByUserIdAndLessonIdIn(USER_ID, List.of(10L))).hasSize(1);
	}

	private UserLessonProgress persistProgress(Long userId, Long lessonId, int completedCount) {
		UserLessonProgress progress = UserLessonProgress.start(userId, lessonId);
		for (int i = 0; i < completedCount; i++) {
			progress.increaseCompletedCount();
		}
		return entityManager.persist(progress);
	}
}
