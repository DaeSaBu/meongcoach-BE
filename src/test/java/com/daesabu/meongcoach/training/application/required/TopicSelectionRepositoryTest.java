package com.daesabu.meongcoach.training.application.required;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.training.domain.TopicSelection;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class TopicSelectionRepositoryTest {

	private static final Long USER_ID = 1L;

	private static final Long OTHER_USER_ID = 2L;

	private static final Long DOG_ID = 7L;

	private static final Long OTHER_DOG_ID = 8L;

	@Autowired
	private TopicSelectionRepository topicSelectionRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 강아지가_선택한_토픽을_조회한다() {
		entityManager.persist(TopicSelection.enter(USER_ID, DOG_ID, 10L));
		flushAndClear();

		Optional<TopicSelection> selectedTopic = topicSelectionRepository.findByUserIdAndDogId(USER_ID, DOG_ID);

		assertThat(selectedTopic).get().extracting(TopicSelection::getTopicId).isEqualTo(10L);
	}

	@Test
	void 선택_기록이_없으면_빈_Optional을_반환한다() {
		Optional<TopicSelection> selectedTopic = topicSelectionRepository.findByUserIdAndDogId(USER_ID, DOG_ID);

		assertThat(selectedTopic).isEmpty();
	}

	@Test
	void 다른_사용자의_선택_기록은_조회되지_않는다() {
		entityManager.persist(TopicSelection.enter(OTHER_USER_ID, DOG_ID, 10L));
		flushAndClear();

		Optional<TopicSelection> selectedTopic = topicSelectionRepository.findByUserIdAndDogId(USER_ID, DOG_ID);

		assertThat(selectedTopic).isEmpty();
	}

	@Test
	void 같은_사용자의_다른_강아지_선택_기록은_조회되지_않는다() {
		entityManager.persist(TopicSelection.enter(USER_ID, OTHER_DOG_ID, 10L));
		flushAndClear();

		Optional<TopicSelection> selectedTopic = topicSelectionRepository.findByUserIdAndDogId(USER_ID, DOG_ID);

		assertThat(selectedTopic).isEmpty();
	}

	@Test
	void 같은_사용자라도_강아지가_다르면_선택_기록을_각각_저장한다() {
		topicSelectionRepository.saveAndFlush(TopicSelection.enter(USER_ID, DOG_ID, 10L));
		topicSelectionRepository.saveAndFlush(TopicSelection.enter(USER_ID, OTHER_DOG_ID, 20L));

		assertThat(topicSelectionRepository.findByUserIdAndDogId(USER_ID, OTHER_DOG_ID))
				.get().extracting(TopicSelection::getTopicId).isEqualTo(20L);
	}

	@Test
	void 한_강아지의_선택_기록을_두_건_저장하면_실패한다() {
		topicSelectionRepository.saveAndFlush(TopicSelection.enter(USER_ID, DOG_ID, 10L));

		assertThatThrownBy(() -> topicSelectionRepository.saveAndFlush(TopicSelection.enter(USER_ID, DOG_ID, 20L)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
