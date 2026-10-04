package com.daesabu.meongcoach.progress.application;

import com.daesabu.meongcoach.progress.application.provided.TopicProgressUpdater;
import com.daesabu.meongcoach.progress.application.required.TopicProgressRepository;
import com.daesabu.meongcoach.progress.domain.TopicProgress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopicProgressModifyService implements TopicProgressUpdater {
	private final TopicProgressRepository topicProgressRepository;

	@Override
	@Transactional
	public void enterTopic(Long userId, Long topicId) {
		topicProgressRepository.findByUserId(userId)
				.ifPresentOrElse(
						selectedTopic -> selectedTopic.moveTo(topicId),
						() -> topicProgressRepository.save(TopicProgress.enter(userId, topicId)));
	}
}
