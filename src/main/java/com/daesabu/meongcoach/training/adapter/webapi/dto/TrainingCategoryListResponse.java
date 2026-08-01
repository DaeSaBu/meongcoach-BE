package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.TrainingCategoryView;
import java.util.List;

/**
 * 라이브러리 탭 진입 응답. 교육 카테고리 전체를 담는다.
 */
public record TrainingCategoryListResponse(List<TrainingCategoryResponse> trainingCategories) {

	public static TrainingCategoryListResponse from(List<TrainingCategoryView> views) {
		List<TrainingCategoryResponse> trainingCategories = views.stream()
				.map(TrainingCategoryResponse::from)
				.toList();
		return new TrainingCategoryListResponse(trainingCategories);
	}
}
