package com.daesabu.meongcoach.progress.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.progress.application.required.UserLessonProgressRepository;
import com.daesabu.meongcoach.progress.domain.UserLessonProgress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("레슨 진행도 서비스")
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
	@DisplayName("완료 횟수가 1 이상인 레슨의 id만 반환한다")
	void findCompletedLessonIdsReturnsOnlyLessonsCompletedAtLeastOnce() {
		persistProgress(USER_ID, 10L, 1);
		persistProgress(USER_ID, 20L, 0);
		entityManager.flush();

		Set<Long> completedLessonIds = lessonProgressService.findCompletedLessonIds(USER_ID, List.of(10L, 20L));

		assertThat(completedLessonIds).containsExactly(10L);
	}

	@Test
	@DisplayName("다른 사용자가 완료한 레슨의 id는 반환하지 않는다")
	void findCompletedLessonIdsExcludesLessonsCompletedByOtherUsers() {
		persistProgress(OTHER_USER_ID, 10L, 3);
		entityManager.flush();

		Set<Long> completedLessonIds = lessonProgressService.findCompletedLessonIds(USER_ID, List.of(10L));

		assertThat(completedLessonIds).isEmpty();
	}

	@Test
	@DisplayName("진행 기록이 없는 레슨의 완료 횟수는 0으로 조회된다")
	void findCompletedCountsTreatsLessonWithoutProgressAsZero() {
		persistProgress(USER_ID, 10L, 2);
		entityManager.flush();

		Map<Long, Integer> completedCounts = lessonProgressService.findCompletedCounts(USER_ID, List.of(10L, 20L));

		assertThat(completedCounts).containsExactlyInAnyOrderEntriesOf(Map.of(10L, 2, 20L, 0));
	}

	@Test
	@DisplayName("다른 사용자의 완료 횟수는 조회되지 않는다")
	void findCompletedCountsExcludesProgressOfOtherUsers() {
		persistProgress(OTHER_USER_ID, 10L, 3);
		entityManager.flush();

		Map<Long, Integer> completedCounts = lessonProgressService.findCompletedCounts(USER_ID, List.of(10L));

		assertThat(completedCounts).containsExactlyInAnyOrderEntriesOf(Map.of(10L, 0));
	}

	@Test
	@DisplayName("첫 완료 시 진행 기록이 생성되고 완료 횟수 1을 반환한다")
	void completeLessonCreatesProgressWithCountOne() {
		int completedCount = lessonProgressService.completeLesson(USER_ID, 10L);

		entityManager.flush();
		entityManager.clear();
		assertThat(completedCount).isEqualTo(1);
		assertThat(userLessonProgressRepository.findByUserIdAndLessonId(USER_ID, 10L))
				.get().extracting(UserLessonProgress::getCompletedCount).isEqualTo(1);
	}

	@Test
	@DisplayName("반복 완료 시 호출할 때마다 완료 횟수가 1씩 증가한다")
	void completeLessonIncreasesCompletedCountByOnePerCall() {
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
	@DisplayName("반복 완료해도 진행 기록은 한 건만 유지된다")
	void completeLessonKeepsSingleProgressRow() {
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
