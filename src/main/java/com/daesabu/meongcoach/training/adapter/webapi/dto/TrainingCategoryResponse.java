package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.TrainingCategoryView;
import java.util.List;

/**
 * 훈련 카테고리 응답. 소속 토픽을 함께 담는다.
 */
public record TrainingCategoryResponse(
		Long trainingCategoryId,
		String trainingCategoryTitle,
		int trainingCategorySortOrder,
		List<TopicResponse> topics
) {

	public static TrainingCategoryResponse from(TrainingCategoryView view) {
		List<TopicResponse> topics = view.topics().stream()
				.map(TopicResponse::from)
				.toList();
		return new TrainingCategoryResponse(view.id(), view.title(), view.sortOrder(), topics);
	}
}
