package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.training.application.provided.TopicFinder;
import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.exception.TopicNotFoundException;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopicQueryService implements TopicFinder {
	private final TopicRepository topicRepository;

	@Override
	public List<TopicResult> findAllOrdered() {
		return topicRepository.findAllOrdered().stream()
				.map(TopicResult::from)
				.toList();
	}

	@Override
	public List<TopicResult> findTopics(Collection<Long> topicIds) {
		return topicRepository.findAllById(topicIds).stream()
				.map(TopicResult::from)
				.toList();
	}

	@Override
	public TopicResult findTopic(Long topicId) {
		Topic topic = topicRepository.findById(topicId)
				.orElseThrow(() -> new TopicNotFoundException(topicId));
		return TopicResult.from(topic);
	}
}
