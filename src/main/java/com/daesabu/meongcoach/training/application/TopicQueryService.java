package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.training.application.provided.TopicFinder;
import com.daesabu.meongcoach.training.application.provided.TopicSummary;
import com.daesabu.meongcoach.training.application.required.TrainingCategoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopicQueryService implements TopicFinder {

	private final TrainingCategoryRepository trainingCategoryRepository;

	@Override
	public List<TopicSummary> findAllOrdered() {
		return trainingCategoryRepository.findAll().stream()
				.flatMap(category -> category.getTopics().stream())
				.map(TopicSummary::from)
				.toList();
	}
}
