package com.daesabu.meongcoach.training.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.application.provided.dto.TopicSelectionRequest;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.application.required.TopicSelectionRepository;
import com.daesabu.meongcoach.training.application.required.TrainingCategoryRepository;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicFixture;
import com.daesabu.meongcoach.training.domain.TopicSelection;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.TrainingCategoryFixture;
import com.daesabu.meongcoach.training.domain.exception.TopicNotFoundException;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class TopicSelectorTest {

	private static final Long USER_ID = 1L;

	private static final Long ABSENT_TOPIC_ID = 999L;

	@Autowired
	private TopicSelector topicSelector;

	@Autowired
	private TrainingCategoryRepository trainingCategoryRepository;

	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private TopicSelectionRepository topicSelectionRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 진입_기록이_없으면_새로_생성한다() {
		Topic topic = saveTopic("앉아", 1);
		flushAndClear();

		topicSelector.selectTopic(USER_ID, new TopicSelectionRequest(topic.getId()));

		flushAndClear();
		assertThat(topicSelectionRepository.count()).isOne();
		assertThat(findEnteredTopicId(USER_ID)).isEqualTo(topic.getId());
	}

	@Test
	void 같은_토픽을_다시_선택해도_진입_기록은_한_건만_유지된다() {
		Topic topic = saveTopic("앉아", 1);
		flushAndClear();

		topicSelector.selectTopic(USER_ID, new TopicSelectionRequest(topic.getId()));
		topicSelector.selectTopic(USER_ID, new TopicSelectionRequest(topic.getId()));

		flushAndClear();
		assertThat(topicSelectionRepository.count()).isOne();
	}

	@Test
	void 다른_토픽을_선택하면_진입_기록의_토픽이_바뀐다() {
		Topic sit = saveTopic("앉아", 1);
		Topic wait = saveTopic("기다려", 2);
		flushAndClear();

		topicSelector.selectTopic(USER_ID, new TopicSelectionRequest(sit.getId()));
		topicSelector.selectTopic(USER_ID, new TopicSelectionRequest(wait.getId()));

		flushAndClear();
		assertThat(topicSelectionRepository.count()).isOne();
		assertThat(findEnteredTopicId(USER_ID)).isEqualTo(wait.getId());
	}

	@Test
	void 존재하지_않는_토픽이면_예외를_던진다() {
		assertThatThrownBy(() -> topicSelector.selectTopic(USER_ID, new TopicSelectionRequest(ABSENT_TOPIC_ID)))
				.isInstanceOf(TopicNotFoundException.class);
	}

	@Test
	void 존재하지_않는_토픽이면_진입_기록을_만들지_않는다() {
		assertThatThrownBy(() -> topicSelector.selectTopic(USER_ID, new TopicSelectionRequest(ABSENT_TOPIC_ID)))
				.isInstanceOf(TopicNotFoundException.class);

		flushAndClear();
		assertThat(topicSelectionRepository.count()).isZero();
	}

	@Test
	void 다른_토픽을_선택하면_수정_시각이_갱신된다() {
		Topic first = saveTopic("앉아", 1);
		Topic second = saveTopic("기다려", 2);
		topicSelector.selectTopic(USER_ID, new TopicSelectionRequest(first.getId()));
		LocalDateTime before = LocalDateTime.of(2026, 1, 1, 0, 0);
		backdateSelection(before);

		topicSelector.selectTopic(USER_ID, new TopicSelectionRequest(second.getId()));
		flushAndClear();

		assertThat(topicSelectionRepository.findByUserId(USER_ID).orElseThrow().getUpdatedAt()).isAfter(before);
	}

	@Test
	void 같은_토픽을_다시_선택하면_수정_시각을_변경하지_않는다() {
		Topic topic = saveTopic("앉아", 1);
		topicSelector.selectTopic(USER_ID, new TopicSelectionRequest(topic.getId()));
		LocalDateTime before = LocalDateTime.of(2026, 1, 1, 0, 0);
		backdateSelection(before);

		topicSelector.selectTopic(USER_ID, new TopicSelectionRequest(topic.getId()));
		flushAndClear();

		assertThat(topicSelectionRepository.findByUserId(USER_ID).orElseThrow().getUpdatedAt()).isEqualTo(before);
	}

	private void backdateSelection(LocalDateTime updatedAt) {
		entityManager.flush();
		entityManager.createQuery("update TopicSelection s set s.updatedAt = :updatedAt where s.userId = :userId")
				.setParameter("updatedAt", updatedAt)
				.setParameter("userId", USER_ID)
				.executeUpdate();
		entityManager.clear();
	}

	private Topic saveTopic(String title, int sortOrder) {
		TrainingCategory category = trainingCategoryRepository.save(TrainingCategoryFixture.create("기본 교육", 1, null, null));
		return topicRepository.save(TopicFixture.create(category, title, sortOrder, null, null, null));
	}

	private Long findEnteredTopicId(Long userId) {
		return topicSelectionRepository.findByUserId(userId)
				.map(TopicSelection::getTopicId)
				.orElseThrow();
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
