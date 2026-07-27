package com.daesabu.meongcoach.training.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.progress.application.TopicEntryService;
import com.daesabu.meongcoach.training.application.provided.TopicSelector;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicCreateCommand;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.exception.TopicNotFoundException;
import java.time.LocalDateTime;
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
		assertThat(findUpdatedAt(USER_ID, topic.getId())).isNotNull();
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
	@DisplayName("같은 토픽을 다시 선택하면 수정 시각이 갱신된다")
	void selectTopicRenewsUpdatedAtOnReselect() {
		Topic topic = persistTopic("앉아", 1);
		flushAndClear();
		topicSelector.selectTopic(USER_ID, topic.getId());
		entityManager.flush();
		LocalDateTime backdated = LocalDateTime.of(2026, 1, 1, 0, 0);
		backdateUpdatedAt(USER_ID, topic.getId(), backdated);
		entityManager.clear();

		topicSelector.selectTopic(USER_ID, topic.getId());

		flushAndClear();
		assertThat(findUpdatedAt(USER_ID, topic.getId())).isAfter(backdated);
	}

	@Test
	@DisplayName("다른 토픽을 선택하면 진입 기록이 따로 생성된다")
	void selectTopicCreatesSeparateEntryForAnotherTopic() {
		Topic sit = persistTopic("앉아", 1);
		Topic wait = persistTopic("기다려", 2);
		flushAndClear();

		topicSelector.selectTopic(USER_ID, sit.getId());
		topicSelector.selectTopic(USER_ID, wait.getId());

		flushAndClear();
		assertThat(countEntries()).isEqualTo(2);
		assertThat(findUpdatedAt(USER_ID, wait.getId())).isNotNull();
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
		TrainingCategory category = entityManager.persist(TrainingCategory.create("기본 훈련", 1));
		return entityManager.persist(Topic.create(category, new TopicCreateCommand(title, sortOrder)));
	}

	private long countEntries() {
		return entityManager.getEntityManager()
				.createQuery("select count(p) from UserCurriculumProgress p", Long.class)
				.getSingleResult();
	}

	private LocalDateTime findUpdatedAt(Long userId, Long topicId) {
		return entityManager.getEntityManager()
				.createQuery("select p.updatedAt from UserCurriculumProgress p "
						+ "where p.userId = :userId and p.topicId = :topicId", LocalDateTime.class)
				.setParameter("userId", userId)
				.setParameter("topicId", topicId)
				.getSingleResult();
	}

	/**
	 * 수정 시각을 명시적으로 지정한다. {@code LocalDateTime.now()}는 연속 호출 시 같은 값이 나올 수 있어 갱신 검증이 흔들린다.
	 */
	private void backdateUpdatedAt(Long userId, Long topicId, LocalDateTime updatedAt) {
		entityManager.getEntityManager()
				.createQuery("update UserCurriculumProgress p set p.updatedAt = :updatedAt "
						+ "where p.userId = :userId and p.topicId = :topicId")
				.setParameter("updatedAt", updatedAt)
				.setParameter("userId", userId)
				.setParameter("topicId", topicId)
				.executeUpdate();
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
