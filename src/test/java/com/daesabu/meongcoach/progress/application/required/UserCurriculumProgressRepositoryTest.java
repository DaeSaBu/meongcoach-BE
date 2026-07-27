package com.daesabu.meongcoach.progress.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.progress.domain.UserCurriculumProgress;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * 사용자별 토픽 진입 기록 조회 리포지토리 검증.
 */
@DataJpaTest
@DisplayName("토픽 진입 기록 리포지토리")
class UserCurriculumProgressRepositoryTest {

	private static final Long USER_ID = 1L;

	private static final Long OTHER_USER_ID = 2L;

	@Autowired
	private UserCurriculumProgressRepository userCurriculumProgressRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("수정 시각이 가장 최신인 토픽 진입 기록을 조회한다")
	void findFirstByUserIdOrderByUpdatedAtDescReturnsLatestEnteredProgress() {
		UserCurriculumProgress older = persistProgress(USER_ID, 10L);
		UserCurriculumProgress latest = persistProgress(USER_ID, 20L);
		entityManager.flush();
		updateUpdatedAt(older, LocalDateTime.of(2026, 1, 1, 0, 0));
		updateUpdatedAt(latest, LocalDateTime.of(2026, 1, 2, 0, 0));
		entityManager.clear();

		Optional<UserCurriculumProgress> progress = userCurriculumProgressRepository
				.findFirstByUserIdOrderByUpdatedAtDesc(USER_ID);

		assertThat(progress).get().extracting(UserCurriculumProgress::getTopicId).isEqualTo(20L);
	}

	@Test
	@DisplayName("진입 기록이 없으면 빈 Optional을 반환한다")
	void findFirstByUserIdOrderByUpdatedAtDescReturnsEmptyWhenProgressIsAbsent() {
		persistProgress(OTHER_USER_ID, 10L);
		entityManager.flush();

		Optional<UserCurriculumProgress> progress = userCurriculumProgressRepository
				.findFirstByUserIdOrderByUpdatedAtDesc(USER_ID);

		assertThat(progress).isEmpty();
	}

	@Test
	@DisplayName("다른 사용자의 진입 기록은 조회되지 않는다")
	void findFirstByUserIdOrderByUpdatedAtDescExcludesProgressOfOtherUsers() {
		UserCurriculumProgress own = persistProgress(USER_ID, 10L);
		UserCurriculumProgress other = persistProgress(OTHER_USER_ID, 20L);
		entityManager.flush();
		updateUpdatedAt(own, LocalDateTime.of(2026, 1, 1, 0, 0));
		updateUpdatedAt(other, LocalDateTime.of(2026, 1, 2, 0, 0));
		entityManager.clear();

		Optional<UserCurriculumProgress> progress = userCurriculumProgressRepository
				.findFirstByUserIdOrderByUpdatedAtDesc(USER_ID);

		assertThat(progress).get().extracting(UserCurriculumProgress::getTopicId).isEqualTo(10L);
	}

	@Test
	@DisplayName("사용자와 토픽으로 진입 기록 한 건을 조회한다")
	void findByUserIdAndTopicIdReturnsProgress() {
		UserCurriculumProgress saved = persistProgress(USER_ID, 10L);
		entityManager.flush();

		Optional<UserCurriculumProgress> progress = userCurriculumProgressRepository
				.findByUserIdAndTopicId(USER_ID, 10L);

		assertThat(progress).get().extracting(UserCurriculumProgress::getId).isEqualTo(saved.getId());
	}

	@Test
	@DisplayName("해당 토픽에 진입한 기록이 없으면 빈 Optional을 반환한다")
	void findByUserIdAndTopicIdReturnsEmptyWhenProgressIsAbsent() {
		persistProgress(USER_ID, 10L);
		entityManager.flush();

		Optional<UserCurriculumProgress> progress = userCurriculumProgressRepository
				.findByUserIdAndTopicId(USER_ID, 99L);

		assertThat(progress).isEmpty();
	}

	@Test
	@DisplayName("같은 토픽이라도 다른 사용자의 진입 기록은 조회되지 않는다")
	void findByUserIdAndTopicIdExcludesProgressOfOtherUsers() {
		persistProgress(OTHER_USER_ID, 10L);
		entityManager.flush();

		Optional<UserCurriculumProgress> progress = userCurriculumProgressRepository
				.findByUserIdAndTopicId(USER_ID, 10L);

		assertThat(progress).isEmpty();
	}

	private UserCurriculumProgress persistProgress(Long userId, Long topicId) {
		return entityManager.persist(UserCurriculumProgress.enter(userId, topicId));
	}

	/**
	 * 수정 시각을 명시적으로 지정한다. {@code LocalDateTime.now()}는 연속 호출 시 같은 값이 나올 수 있어 정렬 검증이 흔들린다.
	 */
	private void updateUpdatedAt(UserCurriculumProgress progress, LocalDateTime updatedAt) {
		entityManager.getEntityManager()
				.createQuery("update UserCurriculumProgress p set p.updatedAt = :updatedAt where p.id = :id")
				.setParameter("updatedAt", updatedAt)
				.setParameter("id", progress.getId())
				.executeUpdate();
	}
}
