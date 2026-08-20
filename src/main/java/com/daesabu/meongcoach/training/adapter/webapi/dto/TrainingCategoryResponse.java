package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.TrainingCategoryResult;
import java.util.List;

/**
 * 교육 카테고리 응답. 소속 토픽을 함께 담는다.
 */
public record TrainingCategoryResponse(
		Long trainingCategoryId,
		String trainingCategoryTitle,
		String trainingCategoryDescription,
		String trainingCategoryIconUrl,
		int trainingCategorySortOrder,
		List<TopicResponse> topics
) {

	public static TrainingCategoryResponse from(TrainingCategoryResult result) {
		List<TopicResponse> topics = result.topics().stream()
				.map(TopicResponse::from)
				.toList();
		return new TrainingCategoryResponse(
				result.id(),
				result.title(),
				result.description(),
				result.iconUrl(),
				result.sortOrder(),
				topics
		);
	}
}
