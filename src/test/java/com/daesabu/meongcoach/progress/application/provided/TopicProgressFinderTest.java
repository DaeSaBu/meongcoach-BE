package com.daesabu.meongcoach.progress.application.provided;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.progress.application.required.TopicProgressRepository;
import com.daesabu.meongcoach.progress.domain.TopicProgress;
import com.daesabu.meongcoach.support.ApplicationTest;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class TopicProgressFinderTest {

	private static final Long USER_ID = 1L;

	private static final Long OTHER_USER_ID = 2L;

	@Autowired
	private TopicProgressFinder topicProgressFinder;

	@Autowired
	private TopicProgressRepository topicProgressRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 진입_기록이_없으면_빈_Optional을_반환한다() {
		Optional<Long> topicId = topicProgressFinder.findLatestTopicId(USER_ID);

		assertThat(topicId).isEmpty();
	}

	@Test
	void 진입_기록의_토픽_id를_반환한다() {
		topicProgressRepository.save(TopicProgress.enter(USER_ID, 20L));
		flushAndClear();

		Optional<Long> topicId = topicProgressFinder.findLatestTopicId(USER_ID);

		assertThat(topicId).contains(20L);
	}

	@Test
	void 다른_사용자의_진입_기록은_반환하지_않는다() {
		topicProgressRepository.save(TopicProgress.enter(OTHER_USER_ID, 10L));
		flushAndClear();

		Optional<Long> topicId = topicProgressFinder.findLatestTopicId(USER_ID);

		assertThat(topicId).isEmpty();
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
