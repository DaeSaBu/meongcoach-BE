package com.daesabu.meongcoach.progress.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.progress.domain.UserLessonProgress;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * 사용자별 레슨 진행도 조회 리포지토리 검증.
 */
@DataJpaTest
class UserLessonProgressRepositoryTest {

	private static final Long USER_ID = 1L;

	private static final Long OTHER_USER_ID = 2L;

	@Autowired
	private UserLessonProgressRepository userLessonProgressRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 사용자의_여러_레슨_진행도를_한_번에_조회한다() {
		persistProgress(USER_ID, 10L);
		persistProgress(USER_ID, 20L);
		entityManager.flush();

		List<UserLessonProgress> progresses = userLessonProgressRepository
				.findAllByUserIdAndLessonIdIn(USER_ID, List.of(10L, 20L));

		assertThat(progresses).extracting(UserLessonProgress::getLessonId)
				.containsExactlyInAnyOrder(10L, 20L);
	}

	@Test
	void 조회_대상에_없는_레슨의_진행도는_조회되지_않는다() {
		persistProgress(USER_ID, 10L);
		persistProgress(USER_ID, 20L);
		entityManager.flush();

		List<UserLessonProgress> progresses = userLessonProgressRepository
				.findAllByUserIdAndLessonIdIn(USER_ID, List.of(10L));

		assertThat(progresses).extracting(UserLessonProgress::getLessonId)
				.containsExactly(10L);
	}

	@Test
	void 다른_사용자의_레슨_진행도는_조회되지_않는다() {
		persistProgress(USER_ID, 10L);
		persistProgress(OTHER_USER_ID, 10L);
		entityManager.flush();

		List<UserLessonProgress> progresses = userLessonProgressRepository
				.findAllByUserIdAndLessonIdIn(USER_ID, List.of(10L));

		assertThat(progresses).extracting(UserLessonProgress::getUserId)
				.containsExactly(USER_ID);
	}

	@Test
	void 레슨_id_목록이_비어_있으면_빈_목록을_반환한다() {
		persistProgress(USER_ID, 10L);
		entityManager.flush();

		List<UserLessonProgress> progresses = userLessonProgressRepository
				.findAllByUserIdAndLessonIdIn(USER_ID, List.of());

		assertThat(progresses).isEmpty();
	}

	@Test
	void 사용자와_레슨으로_진행도_한_건을_조회한다() {
		UserLessonProgress saved = persistProgress(USER_ID, 10L);
		entityManager.flush();

		Optional<UserLessonProgress> progress = userLessonProgressRepository.findByUserIdAndLessonId(USER_ID, 10L);

		assertThat(progress).get().extracting(UserLessonProgress::getId).isEqualTo(saved.getId());
	}

	@Test
	void 진행_기록이_없으면_빈_Optional을_반환한다() {
		persistProgress(USER_ID, 10L);
		entityManager.flush();

		Optional<UserLessonProgress> progress = userLessonProgressRepository.findByUserIdAndLessonId(USER_ID, 99L);

		assertThat(progress).isEmpty();
	}

	@Test
	void 같은_레슨이라도_다른_사용자의_진행도는_조회되지_않는다() {
		persistProgress(OTHER_USER_ID, 10L);
		entityManager.flush();

		Optional<UserLessonProgress> progress = userLessonProgressRepository.findByUserIdAndLessonId(USER_ID, 10L);

		assertThat(progress).isEmpty();
	}

	private UserLessonProgress persistProgress(Long userId, Long lessonId) {
		return entityManager.persist(UserLessonProgress.start(userId, lessonId));
	}
}
