package com.daesabu.meongcoach.progress.application.required;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.progress.domain.UserSelectedTopic;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * 사용자별 선택 토픽 조회 리포지토리 검증.
 */
@DataJpaTest
class UserSelectedTopicRepositoryTest {

	private static final Long USER_ID = 1L;

	private static final Long OTHER_USER_ID = 2L;

	@Autowired
	private UserSelectedTopicRepository userSelectedTopicRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 사용자가_선택한_토픽을_조회한다() {
		entityManager.persist(UserSelectedTopic.enter(USER_ID, 10L));
		flushAndClear();

		Optional<UserSelectedTopic> selectedTopic = userSelectedTopicRepository.findByUserId(USER_ID);

		assertThat(selectedTopic).get().extracting(UserSelectedTopic::getTopicId).isEqualTo(10L);
	}

	@Test
	void 선택_기록이_없으면_빈_Optional을_반환한다() {
		Optional<UserSelectedTopic> selectedTopic = userSelectedTopicRepository.findByUserId(USER_ID);

		assertThat(selectedTopic).isEmpty();
	}

	@Test
	void 다른_사용자의_선택_기록은_조회되지_않는다() {
		entityManager.persist(UserSelectedTopic.enter(OTHER_USER_ID, 10L));
		flushAndClear();

		Optional<UserSelectedTopic> selectedTopic = userSelectedTopicRepository.findByUserId(USER_ID);

		assertThat(selectedTopic).isEmpty();
	}

	@Test
	void 한_사용자의_선택_기록을_두_건_저장하면_실패한다() {
		userSelectedTopicRepository.saveAndFlush(UserSelectedTopic.enter(USER_ID, 10L));

		assertThatThrownBy(() -> userSelectedTopicRepository.saveAndFlush(UserSelectedTopic.enter(USER_ID, 20L)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
