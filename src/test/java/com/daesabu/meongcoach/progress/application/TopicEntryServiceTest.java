package com.daesabu.meongcoach.progress.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.progress.application.required.UserSelectedTopicRepository;
import com.daesabu.meongcoach.progress.domain.UserSelectedTopic;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * 토픽 진입 모듈 공개 API 검증.
 */
@DataJpaTest
@Import(TopicEntryService.class)
@DisplayName("토픽 진입 서비스")
class TopicEntryServiceTest {

	private static final Long USER_ID = 1L;

	private static final Long OTHER_USER_ID = 2L;

	private static final LocalDateTime BACKDATED = LocalDateTime.of(2026, 1, 1, 0, 0);

	@Autowired
	private TopicEntryService topicEntryService;

	@Autowired
	private UserSelectedTopicRepository userSelectedTopicRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("진입 기록이 없으면 빈 Optional을 반환한다")
	void findLatestEnteredTopicIdReturnsEmptyWhenNoEntryExists() {
		Optional<Long> topicId = topicEntryService.findLatestEnteredTopicId(USER_ID);

		assertThat(topicId).isEmpty();
	}

	@Test
	@DisplayName("두 토픽에 순차 진입하면 마지막에 진입한 토픽을 반환한다")
	void findLatestEnteredTopicIdReturnsLastEnteredTopic() {
		topicEntryService.enterTopic(USER_ID, 10L);
		topicEntryService.enterTopic(USER_ID, 20L);
		flushAndClear();

		Optional<Long> topicId = topicEntryService.findLatestEnteredTopicId(USER_ID);

		assertThat(topicId).contains(20L);
	}

	@Test
	@DisplayName("이미 진입했던 토픽에 다시 진입하면 그 토픽이 가장 최신 진입 토픽이 된다")
	void findLatestEnteredTopicIdReturnsReenteredTopic() {
		topicEntryService.enterTopic(USER_ID, 10L);
		topicEntryService.enterTopic(USER_ID, 20L);
		flushAndClear();

		topicEntryService.enterTopic(USER_ID, 10L);
		flushAndClear();

		assertThat(topicEntryService.findLatestEnteredTopicId(USER_ID)).contains(10L);
	}

	@Test
	@DisplayName("다른 사용자의 진입 기록은 반환하지 않는다")
	void findLatestEnteredTopicIdExcludesEntryOfOtherUsers() {
		topicEntryService.enterTopic(OTHER_USER_ID, 10L);
		flushAndClear();

		Optional<Long> topicId = topicEntryService.findLatestEnteredTopicId(USER_ID);

		assertThat(topicId).isEmpty();
	}

	@Test
	@DisplayName("진입 기록이 없으면 새로 생성한다")
	void enterTopicCreatesEntryWhenAbsent() {
		topicEntryService.enterTopic(USER_ID, 10L);

		flushAndClear();
		assertThat(userSelectedTopicRepository.findByUserId(USER_ID))
				.get().extracting(UserSelectedTopic::getTopicId).isEqualTo(10L);
	}

	@Test
	@DisplayName("여러 토픽에 진입해도 진입 기록은 한 건만 유지된다")
	void enterTopicKeepsSingleEntryRow() {
		topicEntryService.enterTopic(USER_ID, 10L);
		topicEntryService.enterTopic(USER_ID, 20L);
		topicEntryService.enterTopic(USER_ID, 10L);

		flushAndClear();
		assertThat(userSelectedTopicRepository.findAll()).hasSize(1);
	}

	@Test
	@DisplayName("다른 토픽에 진입하면 수정 시각이 갱신된다")
	void enterTopicRenewsUpdatedAtWhenTopicChanges() {
		topicEntryService.enterTopic(USER_ID, 10L);
		entityManager.flush();
		backdateUpdatedAt(USER_ID, BACKDATED);
		entityManager.clear();

		topicEntryService.enterTopic(USER_ID, 20L);
		flushAndClear();

		assertThat(findUpdatedAt(USER_ID)).isAfter(BACKDATED);
	}

	@Test
	@DisplayName("같은 토픽에 다시 진입하면 아무것도 갱신하지 않는다")
	void enterTopicUpdatesNothingWhenTopicIsUnchanged() {
		topicEntryService.enterTopic(USER_ID, 10L);
		entityManager.flush();
		backdateUpdatedAt(USER_ID, BACKDATED);
		entityManager.clear();

		topicEntryService.enterTopic(USER_ID, 10L);
		flushAndClear();

		assertThat(findUpdatedAt(USER_ID)).isEqualTo(BACKDATED);
	}

	private LocalDateTime findUpdatedAt(Long userId) {
		return entityManager.getEntityManager()
				.createQuery("select c.updatedAt from UserSelectedTopic c where c.userId = :userId",
						LocalDateTime.class)
				.setParameter("userId", userId)
				.getSingleResult();
	}

	/**
	 * 수정 시각을 명시적으로 지정한다. {@code LocalDateTime.now()}는 연속 호출 시 같은 값이 나올 수 있어 갱신 검증이 흔들린다.
	 */
	private void backdateUpdatedAt(Long userId, LocalDateTime updatedAt) {
		entityManager.getEntityManager()
				.createQuery("update UserSelectedTopic c set c.updatedAt = :updatedAt where c.userId = :userId")
				.setParameter("updatedAt", updatedAt)
				.setParameter("userId", userId)
				.executeUpdate();
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
