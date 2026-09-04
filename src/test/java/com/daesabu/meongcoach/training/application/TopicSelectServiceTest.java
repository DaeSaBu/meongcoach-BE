package com.daesabu.meongcoach.training.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.progress.application.TopicEntryService;
import com.daesabu.meongcoach.training.application.provided.TopicSelector;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicCreateCommand;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.exception.TopicNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * 커리큘럼 화면 변경 서비스 검증.
 */
@DataJpaTest
@Import({TopicSelectService.class, TopicEntryService.class})
class TopicSelectServiceTest {

	private static final Long USER_ID = 1L;

	private static final Long ABSENT_TOPIC_ID = 999L;

	@Autowired
	private TopicSelector topicSelector;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 진입_기록이_없으면_새로_생성한다() {
		Topic topic = persistTopic("앉아", 1);
		flushAndClear();

		topicSelector.selectTopic(USER_ID, topic.getId());

		flushAndClear();
		assertThat(countEntries()).isOne();
		assertThat(findEnteredTopicId(USER_ID)).isEqualTo(topic.getId());
	}

	@Test
	void 같은_토픽을_다시_선택해도_진입_기록은_한_건만_유지된다() {
		Topic topic = persistTopic("앉아", 1);
		flushAndClear();

		topicSelector.selectTopic(USER_ID, topic.getId());
		topicSelector.selectTopic(USER_ID, topic.getId());

		flushAndClear();
		assertThat(countEntries()).isOne();
	}

	@Test
	void 다른_토픽을_선택하면_진입_기록의_토픽이_바뀐다() {
		Topic sit = persistTopic("앉아", 1);
		Topic wait = persistTopic("기다려", 2);
		flushAndClear();

		topicSelector.selectTopic(USER_ID, sit.getId());
		topicSelector.selectTopic(USER_ID, wait.getId());

		flushAndClear();
		assertThat(countEntries()).isOne();
		assertThat(findEnteredTopicId(USER_ID)).isEqualTo(wait.getId());
	}

	@Test
	void 존재하지_않는_토픽이면_예외를_던진다() {
		assertThatThrownBy(() -> topicSelector.selectTopic(USER_ID, ABSENT_TOPIC_ID))
				.isInstanceOf(TopicNotFoundException.class);
	}

	@Test
	void 존재하지_않는_토픽이면_진입_기록을_만들지_않는다() {
		assertThatThrownBy(() -> topicSelector.selectTopic(USER_ID, ABSENT_TOPIC_ID))
				.isInstanceOf(TopicNotFoundException.class);

		flushAndClear();
		assertThat(countEntries()).isZero();
	}

	private Topic persistTopic(String title, int sortOrder) {
		TrainingCategory category = entityManager.persist(TrainingCategory.create("기본 교육", 1, null, null));
		return entityManager.persist(Topic.create(category, new TopicCreateCommand(title, sortOrder, null, null, null)));
	}

	private long countEntries() {
		return entityManager.getEntityManager()
				.createQuery("select count(c) from UserSelectedTopic c", Long.class)
				.getSingleResult();
	}

	private Long findEnteredTopicId(Long userId) {
		return entityManager.getEntityManager()
				.createQuery("select c.topicId from UserSelectedTopic c where c.userId = :userId", Long.class)
				.setParameter("userId", userId)
				.getSingleResult();
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
