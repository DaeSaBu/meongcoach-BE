package com.daesabu.meongcoach.progress.application.provided;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.progress.application.required.TopicProgressRepository;
import com.daesabu.meongcoach.progress.domain.TopicProgress;
import com.daesabu.meongcoach.support.ApplicationTest;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class TopicProgressUpdaterTest {

	private static final Long USER_ID = 1L;

	private static final LocalDateTime BACKDATED = LocalDateTime.of(2026, 1, 1, 0, 0);

	@Autowired
	private TopicProgressUpdater topicProgressUpdater;

	@Autowired
	private TopicProgressRepository topicProgressRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 진입_기록이_없으면_새로_생성한다() {
		topicProgressUpdater.enterTopic(USER_ID, 10L);

		flushAndClear();
		assertThat(topicProgressRepository.findByUserId(USER_ID))
				.get().extracting(TopicProgress::getTopicId).isEqualTo(10L);
	}

	@Test
	void 두_토픽에_순차_진입하면_마지막에_진입한_토픽이_기록된다() {
		topicProgressUpdater.enterTopic(USER_ID, 10L);
		topicProgressUpdater.enterTopic(USER_ID, 20L);

		flushAndClear();
		assertThat(topicProgressRepository.findByUserId(USER_ID))
				.get().extracting(TopicProgress::getTopicId).isEqualTo(20L);
	}

	@Test
	void 이미_진입했던_토픽에_다시_진입하면_그_토픽이_기록된다() {
		topicProgressUpdater.enterTopic(USER_ID, 10L);
		topicProgressUpdater.enterTopic(USER_ID, 20L);
		flushAndClear();

		topicProgressUpdater.enterTopic(USER_ID, 10L);

		flushAndClear();
		assertThat(topicProgressRepository.findByUserId(USER_ID))
				.get().extracting(TopicProgress::getTopicId).isEqualTo(10L);
	}

	@Test
	void 여러_토픽에_진입해도_진입_기록은_한_건만_유지된다() {
		topicProgressUpdater.enterTopic(USER_ID, 10L);
		topicProgressUpdater.enterTopic(USER_ID, 20L);
		topicProgressUpdater.enterTopic(USER_ID, 10L);

		flushAndClear();
		assertThat(topicProgressRepository.findAll()).hasSize(1);
	}

	@Test
	void 다른_토픽에_진입하면_수정_시각이_갱신된다() {
		topicProgressUpdater.enterTopic(USER_ID, 10L);
		entityManager.flush();
		backdateUpdatedAt(USER_ID, BACKDATED);
		entityManager.clear();

		topicProgressUpdater.enterTopic(USER_ID, 20L);
		flushAndClear();

		assertThat(findUpdatedAt(USER_ID)).isAfter(BACKDATED);
	}

	@Test
	void 같은_토픽에_다시_진입하면_아무것도_갱신하지_않는다() {
		topicProgressUpdater.enterTopic(USER_ID, 10L);
		entityManager.flush();
		backdateUpdatedAt(USER_ID, BACKDATED);
		entityManager.clear();

		topicProgressUpdater.enterTopic(USER_ID, 10L);
		flushAndClear();

		assertThat(findUpdatedAt(USER_ID)).isEqualTo(BACKDATED);
	}

	private LocalDateTime findUpdatedAt(Long userId) {
		return entityManager
				.createQuery("select c.updatedAt from TopicProgress c where c.userId = :userId", LocalDateTime.class)
				.setParameter("userId", userId)
				.getSingleResult();
	}

	private void backdateUpdatedAt(Long userId, LocalDateTime updatedAt) {
		entityManager
				.createQuery("update TopicProgress c set c.updatedAt = :updatedAt where c.userId = :userId")
				.setParameter("updatedAt", updatedAt)
				.setParameter("userId", userId)
				.executeUpdate();
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
