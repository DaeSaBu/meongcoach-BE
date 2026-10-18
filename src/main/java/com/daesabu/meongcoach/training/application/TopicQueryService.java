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
		List<Topic> topics = topicRepository.findAllById(topicIds);
		for (Long topicId : topicIds) {
			if (topics.stream().noneMatch(topic -> topic.getId().equals(topicId))) {
				throw new TopicNotFoundException(topicId);
			}
		}
		return topics.stream()
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
