package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.training.application.provided.TopicFinder;
import com.daesabu.meongcoach.training.application.provided.TopicSummary;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 토픽 목록을 정렬 순서대로 조회한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopicFinderService implements TopicFinder {

	private final TopicRepository topicRepository;

	@Override
	public List<TopicSummary> findAllOrdered() {
		return topicRepository.findAllByOrderBySortOrderAsc().stream()
				.map(TopicSummary::from)
				.toList();
	}
}
