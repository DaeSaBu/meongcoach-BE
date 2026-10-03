package com.daesabu.meongcoach.progress.application;

import com.daesabu.meongcoach.progress.application.provided.TopicProgressFinder;
import com.daesabu.meongcoach.progress.application.required.TopicProgressRepository;
import com.daesabu.meongcoach.progress.domain.TopicProgress;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopicProgressQueryService implements TopicProgressFinder {
	private final TopicProgressRepository topicProgressRepository;

	@Override
	public Optional<Long> findLatestTopicId(Long userId) {
		return topicProgressRepository.findByUserId(userId)
				.map(TopicProgress::getTopicId);
	}
}
