package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.training.application.provided.TopicFinder;
import com.daesabu.meongcoach.training.application.provided.TopicSummary;
import com.daesabu.meongcoach.training.application.provided.TopicValidator;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.exception.TopicNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 토픽 목록을 정렬 순서대로 조회한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopicFinderService implements TopicFinder, TopicValidator {

	private final TopicRepository topicRepository;

	@Override
	public List<TopicSummary> findAllOrdered() {
		return topicRepository.findAllByOrderBySortOrderAsc().stream()
				.map(TopicSummary::from)
				.toList();
	}

	@Override
	public void validateAllExist(Set<Long> topicIds) {
		if (topicIds.isEmpty()) {
			return;
		}

		Set<Long> missingTopicIds = new HashSet<>(topicIds);
		topicRepository.findAllById(topicIds).stream()
				.map(Topic::getId)
				.forEach(missingTopicIds::remove);

		missingTopicIds.stream()
				.sorted()
				.findFirst()
				.ifPresent(topicId -> {
					throw new TopicNotFoundException(topicId);
				});
	}
}
