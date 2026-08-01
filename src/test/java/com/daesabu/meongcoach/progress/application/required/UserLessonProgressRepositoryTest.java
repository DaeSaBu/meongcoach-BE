package com.daesabu.meongcoach.progress.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.progress.domain.UserLessonProgress;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * 사용자별 레슨 진행도 조회 리포지토리 검증.
 */
@DataJpaTest
@DisplayName("레슨 진행도 리포지토리")
class UserLessonProgressRepositoryTest {

	private static final Long USER_ID = 1L;

	private static final Long OTHER_USER_ID = 2L;

	@Autowired
	private UserLessonProgressRepository userLessonProgressRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("사용자의 여러 레슨 진행도를 한 번에 조회한다")
	void findAllByUserIdAndLessonIdInReturnsProgressOfGivenLessons() {
		persistProgress(USER_ID, 10L);
		persistProgress(USER_ID, 20L);
		entityManager.flush();

		List<UserLessonProgress> progresses = userLessonProgressRepository
				.findAllByUserIdAndLessonIdIn(USER_ID, List.of(10L, 20L));

		assertThat(progresses).extracting(UserLessonProgress::getLessonId)
				.containsExactlyInAnyOrder(10L, 20L);
	}

	@Test
	@DisplayName("조회 대상에 없는 레슨의 진행도는 조회되지 않는다")
	void findAllByUserIdAndLessonIdInExcludesLessonsOutOfGivenIds() {
		persistProgress(USER_ID, 10L);
		persistProgress(USER_ID, 20L);
		entityManager.flush();

		List<UserLessonProgress> progresses = userLessonProgressRepository
				.findAllByUserIdAndLessonIdIn(USER_ID, List.of(10L));

		assertThat(progresses).extracting(UserLessonProgress::getLessonId)
				.containsExactly(10L);
	}

	@Test
	@DisplayName("다른 사용자의 레슨 진행도는 조회되지 않는다")
	void findAllByUserIdAndLessonIdInExcludesProgressOfOtherUsers() {
		persistProgress(USER_ID, 10L);
		persistProgress(OTHER_USER_ID, 10L);
		entityManager.flush();

		List<UserLessonProgress> progresses = userLessonProgressRepository
				.findAllByUserIdAndLessonIdIn(USER_ID, List.of(10L));

		assertThat(progresses).extracting(UserLessonProgress::getUserId)
				.containsExactly(USER_ID);
	}

	@Test
	@DisplayName("레슨 id 목록이 비어 있으면 빈 목록을 반환한다")
	void findAllByUserIdAndLessonIdInReturnsEmptyListWhenLessonIdsAreEmpty() {
		persistProgress(USER_ID, 10L);
		entityManager.flush();

		List<UserLessonProgress> progresses = userLessonProgressRepository
				.findAllByUserIdAndLessonIdIn(USER_ID, List.of());

		assertThat(progresses).isEmpty();
	}

	@Test
	@DisplayName("사용자와 레슨으로 진행도 한 건을 조회한다")
	void findByUserIdAndLessonIdReturnsProgress() {
		UserLessonProgress saved = persistProgress(USER_ID, 10L);
		entityManager.flush();

		Optional<UserLessonProgress> progress = userLessonProgressRepository.findByUserIdAndLessonId(USER_ID, 10L);

		assertThat(progress).get().extracting(UserLessonProgress::getId).isEqualTo(saved.getId());
	}

	@Test
	@DisplayName("진행 기록이 없으면 빈 Optional을 반환한다")
	void findByUserIdAndLessonIdReturnsEmptyWhenProgressIsAbsent() {
		persistProgress(USER_ID, 10L);
		entityManager.flush();

		Optional<UserLessonProgress> progress = userLessonProgressRepository.findByUserIdAndLessonId(USER_ID, 99L);

		assertThat(progress).isEmpty();
	}

	@Test
	@DisplayName("같은 레슨이라도 다른 사용자의 진행도는 조회되지 않는다")
	void findByUserIdAndLessonIdExcludesProgressOfOtherUsers() {
		persistProgress(OTHER_USER_ID, 10L);
		entityManager.flush();

		Optional<UserLessonProgress> progress = userLessonProgressRepository.findByUserIdAndLessonId(USER_ID, 10L);

		assertThat(progress).isEmpty();
	}

	private UserLessonProgress persistProgress(Long userId, Long lessonId) {
		return entityManager.persist(UserLessonProgress.start(userId, lessonId));
	}
}
