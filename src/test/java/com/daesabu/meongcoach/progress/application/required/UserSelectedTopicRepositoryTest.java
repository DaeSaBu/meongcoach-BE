package com.daesabu.meongcoach.progress.application.required;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.progress.domain.UserSelectedTopic;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * 사용자별 선택 토픽 조회 리포지토리 검증.
 */
@DataJpaTest
@DisplayName("선택 토픽 리포지토리")
class UserSelectedTopicRepositoryTest {

	private static final Long USER_ID = 1L;

	private static final Long OTHER_USER_ID = 2L;

	@Autowired
	private UserSelectedTopicRepository userSelectedTopicRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("사용자가 선택한 토픽을 조회한다")
	void findByUserIdReturnsSelectedTopic() {
		entityManager.persist(UserSelectedTopic.enter(USER_ID, 10L));
		flushAndClear();

		Optional<UserSelectedTopic> selectedTopic = userSelectedTopicRepository.findByUserId(USER_ID);

		assertThat(selectedTopic).get().extracting(UserSelectedTopic::getTopicId).isEqualTo(10L);
	}

	@Test
	@DisplayName("선택 기록이 없으면 빈 Optional을 반환한다")
	void findByUserIdReturnsEmptyWhenSelectedTopicIsAbsent() {
		Optional<UserSelectedTopic> selectedTopic = userSelectedTopicRepository.findByUserId(USER_ID);

		assertThat(selectedTopic).isEmpty();
	}

	@Test
	@DisplayName("다른 사용자의 선택 기록은 조회되지 않는다")
	void findByUserIdExcludesSelectedTopicOfOtherUsers() {
		entityManager.persist(UserSelectedTopic.enter(OTHER_USER_ID, 10L));
		flushAndClear();

		Optional<UserSelectedTopic> selectedTopic = userSelectedTopicRepository.findByUserId(USER_ID);

		assertThat(selectedTopic).isEmpty();
	}

	@Test
	@DisplayName("한 사용자의 선택 기록을 두 건 저장하면 실패한다")
	void saveFailsWhenUserIdIsDuplicated() {
		userSelectedTopicRepository.saveAndFlush(UserSelectedTopic.enter(USER_ID, 10L));

		assertThatThrownBy(() -> userSelectedTopicRepository.saveAndFlush(UserSelectedTopic.enter(USER_ID, 20L)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
