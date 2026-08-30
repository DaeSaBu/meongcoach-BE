package com.daesabu.meongcoach.progress.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.progress.application.required.UserSelectedTopicRepository;
import com.daesabu.meongcoach.progress.domain.UserSelectedTopic;
import java.time.LocalDateTime;
import java.util.Optional;
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
	void 진입_기록이_없으면_빈_Optional을_반환한다() {
		Optional<Long> topicId = topicEntryService.findLatestEnteredTopicId(USER_ID);

		assertThat(topicId).isEmpty();
	}

	@Test
	void 두_토픽에_순차_진입하면_마지막에_진입한_토픽을_반환한다() {
		topicEntryService.enterTopic(USER_ID, 10L);
		topicEntryService.enterTopic(USER_ID, 20L);
		flushAndClear();

		Optional<Long> topicId = topicEntryService.findLatestEnteredTopicId(USER_ID);

		assertThat(topicId).contains(20L);
	}

	@Test
	void 이미_진입했던_토픽에_다시_진입하면_그_토픽이_가장_최신_진입_토픽이_된다() {
		topicEntryService.enterTopic(USER_ID, 10L);
		topicEntryService.enterTopic(USER_ID, 20L);
		flushAndClear();

		topicEntryService.enterTopic(USER_ID, 10L);
		flushAndClear();

		assertThat(topicEntryService.findLatestEnteredTopicId(USER_ID)).contains(10L);
	}

	@Test
	void 다른_사용자의_진입_기록은_반환하지_않는다() {
		topicEntryService.enterTopic(OTHER_USER_ID, 10L);
		flushAndClear();

		Optional<Long> topicId = topicEntryService.findLatestEnteredTopicId(USER_ID);

		assertThat(topicId).isEmpty();
	}

	@Test
	void 진입_기록이_없으면_새로_생성한다() {
		topicEntryService.enterTopic(USER_ID, 10L);

		flushAndClear();
		assertThat(userSelectedTopicRepository.findByUserId(USER_ID))
				.get().extracting(UserSelectedTopic::getTopicId).isEqualTo(10L);
	}

	@Test
	void 여러_토픽에_진입해도_진입_기록은_한_건만_유지된다() {
		topicEntryService.enterTopic(USER_ID, 10L);
		topicEntryService.enterTopic(USER_ID, 20L);
		topicEntryService.enterTopic(USER_ID, 10L);

		flushAndClear();
		assertThat(userSelectedTopicRepository.findAll()).hasSize(1);
	}

	@Test
	void 다른_토픽에_진입하면_수정_시각이_갱신된다() {
		topicEntryService.enterTopic(USER_ID, 10L);
		entityManager.flush();
		backdateUpdatedAt(USER_ID, BACKDATED);
		entityManager.clear();

		topicEntryService.enterTopic(USER_ID, 20L);
		flushAndClear();

		assertThat(findUpdatedAt(USER_ID)).isAfter(BACKDATED);
	}

	@Test
	void 같은_토픽에_다시_진입하면_아무것도_갱신하지_않는다() {
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
