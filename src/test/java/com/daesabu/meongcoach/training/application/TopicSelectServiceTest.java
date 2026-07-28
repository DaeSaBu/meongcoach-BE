package com.daesabu.meongcoach.training.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.progress.application.TopicEntryService;
import com.daesabu.meongcoach.training.application.provided.TopicSelector;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicCreateCommand;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.exception.TopicNotFoundException;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("커리큘럼 화면 변경 서비스")
class TopicSelectServiceTest {

	private static final Long USER_ID = 1L;

	private static final Long ABSENT_TOPIC_ID = 999L;

	@Autowired
	private TopicSelector topicSelector;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("진입 기록이 없으면 새로 생성한다")
	void selectTopicCreatesEntryWhenAbsent() {
		Topic topic = persistTopic("앉아", 1);
		flushAndClear();

		topicSelector.selectTopic(USER_ID, topic.getId());

		flushAndClear();
		assertThat(countEntries()).isOne();
		assertThat(findEnteredTopicId(USER_ID)).isEqualTo(topic.getId());
	}

	@Test
	@DisplayName("같은 토픽을 다시 선택해도 진입 기록은 한 건만 유지된다")
	void selectTopicKeepsSingleEntryOnReselect() {
		Topic topic = persistTopic("앉아", 1);
		flushAndClear();

		topicSelector.selectTopic(USER_ID, topic.getId());
		topicSelector.selectTopic(USER_ID, topic.getId());

		flushAndClear();
		assertThat(countEntries()).isOne();
	}

	@Test
	@DisplayName("다른 토픽을 선택하면 진입 기록의 토픽이 바뀐다")
	void selectTopicMovesEntryToAnotherTopic() {
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
	@DisplayName("존재하지 않는 토픽이면 예외를 던진다")
	void selectTopicThrowsWhenTopicDoesNotExist() {
		assertThatThrownBy(() -> topicSelector.selectTopic(USER_ID, ABSENT_TOPIC_ID))
				.isInstanceOf(TopicNotFoundException.class);
	}

	@Test
	@DisplayName("존재하지 않는 토픽이면 진입 기록을 만들지 않는다")
	void selectTopicDoesNotRecordEntryWhenTopicDoesNotExist() {
		assertThatThrownBy(() -> topicSelector.selectTopic(USER_ID, ABSENT_TOPIC_ID))
				.isInstanceOf(TopicNotFoundException.class);

		flushAndClear();
		assertThat(countEntries()).isZero();
	}

	private Topic persistTopic(String title, int sortOrder) {
		TrainingCategory category = entityManager.persist(TrainingCategory.create("기본 교육", 1));
		return entityManager.persist(Topic.create(category, new TopicCreateCommand(title, sortOrder)));
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
