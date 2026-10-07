package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.training.application.provided.TopicFinder;
import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
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
	public List<TopicResult> findAll() {
		return topicRepository.findAll().stream()
				.map(TopicResult::from)
				.toList();
	}
}
