package com.daesabu.meongcoach.progress.application;

import com.daesabu.meongcoach.progress.application.provided.TopicEntryFinder;
import com.daesabu.meongcoach.progress.application.provided.TopicEntryRecorder;
import com.daesabu.meongcoach.progress.application.required.UserSelectedTopicRepository;
import com.daesabu.meongcoach.progress.domain.UserSelectedTopic;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 토픽 진입 조회·기록 서비스. 모듈 공개 API 두 가지를 함께 구현한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopicEntryService implements TopicEntryFinder, TopicEntryRecorder {

	private final UserSelectedTopicRepository userSelectedTopicRepository;

	@Override
	public Optional<Long> findLatestEnteredTopicId(Long userId) {
		return userSelectedTopicRepository.findByUserId(userId)
				.map(UserSelectedTopic::getTopicId);
	}

	@Override
	@Transactional
	public void enterTopic(Long userId, Long topicId) {
		userSelectedTopicRepository.findByUserId(userId)
				.ifPresentOrElse(
						selectedTopic -> selectedTopic.moveTo(topicId),
						() -> userSelectedTopicRepository.save(UserSelectedTopic.enter(userId, topicId)));
	}
}
