package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.training.application.provided.TopicView;
import com.daesabu.meongcoach.training.application.provided.TrainingCategoryFinder;
import com.daesabu.meongcoach.training.application.provided.TrainingCategoryView;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.application.required.TrainingCategoryRepository;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 훈련 카테고리 조회 서비스. 카테고리·토픽을 각각 한 번씩만 조회하고 메모리에서 그룹핑한다.
 */
@Service
@RequiredArgsConstructor
public class TrainingCategoryQueryService implements TrainingCategoryFinder {

	private final TrainingCategoryRepository trainingCategoryRepository;

	private final TopicRepository topicRepository;

	@Override
	@Transactional(readOnly = true)
	public List<TrainingCategoryView> findAll() {
		List<TrainingCategory> categories = trainingCategoryRepository.findAllByOrderBySortOrderAscIdAsc();
		Map<Long, List<Topic>> topicsByCategoryId = groupTopicsByCategoryId();

		return categories.stream()
				.map(category -> toView(category, topicsByCategoryId.getOrDefault(category.getId(), List.of())))
				.toList();
	}

	// 정렬된 전체 토픽을 한 번에 읽어 카테고리별로 나눈다. 조회 순서가 유지되므로 그룹 안의 정렬도 그대로다
	private Map<Long, List<Topic>> groupTopicsByCategoryId() {
		return topicRepository.findAllByOrderByTrainingCategory_SortOrderAscSortOrderAscIdAsc().stream()
				.collect(Collectors.groupingBy(topic -> topic.getTrainingCategory().getId()));
	}

	private TrainingCategoryView toView(TrainingCategory category, List<Topic> topics) {
		List<TopicView> topicViews = topics.stream()
				.map(topic -> new TopicView(topic.getId(), topic.getTitle(), topic.getSortOrder()))
				.toList();
		return new TrainingCategoryView(category.getId(), category.getTitle(), category.getSortOrder(), topicViews);
	}
}
