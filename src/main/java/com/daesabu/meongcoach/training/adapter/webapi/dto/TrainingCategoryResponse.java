package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.domain.TrainingCategory;
import java.util.List;

public record TrainingCategoryResponse(
		Long trainingCategoryId,
		String trainingCategoryTitle,
		String trainingCategoryDescription,
		String trainingCategoryIconUrl,
		List<TopicResponse> topics
) {

	public static TrainingCategoryResponse from(TrainingCategory category) {
		List<TopicResponse> topics = category.getTopics().stream()
				.map(TopicResponse::from)
				.toList();
		return new TrainingCategoryResponse(
				category.getId(),
				category.getTitle(),
				category.getDescription(),
				category.getIconUrl(),
				topics
		);
	}
}
