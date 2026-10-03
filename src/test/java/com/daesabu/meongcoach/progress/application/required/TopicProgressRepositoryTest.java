package com.daesabu.meongcoach.progress.application.required;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.progress.domain.TopicProgress;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class TopicProgressRepositoryTest {

	private static final Long USER_ID = 1L;

	private static final Long OTHER_USER_ID = 2L;

	@Autowired
	private TopicProgressRepository topicProgressRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 사용자가_선택한_토픽을_조회한다() {
		entityManager.persist(TopicProgress.enter(USER_ID, 10L));
		flushAndClear();

		Optional<TopicProgress> selectedTopic = topicProgressRepository.findByUserId(USER_ID);

		assertThat(selectedTopic).get().extracting(TopicProgress::getTopicId).isEqualTo(10L);
	}

	@Test
	void 선택_기록이_없으면_빈_Optional을_반환한다() {
		Optional<TopicProgress> selectedTopic = topicProgressRepository.findByUserId(USER_ID);

		assertThat(selectedTopic).isEmpty();
	}

	@Test
	void 다른_사용자의_선택_기록은_조회되지_않는다() {
		entityManager.persist(TopicProgress.enter(OTHER_USER_ID, 10L));
		flushAndClear();

		Optional<TopicProgress> selectedTopic = topicProgressRepository.findByUserId(USER_ID);

		assertThat(selectedTopic).isEmpty();
	}

	@Test
	void 한_사용자의_선택_기록을_두_건_저장하면_실패한다() {
		topicProgressRepository.saveAndFlush(TopicProgress.enter(USER_ID, 10L));

		assertThatThrownBy(() -> topicProgressRepository.saveAndFlush(TopicProgress.enter(USER_ID, 20L)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
