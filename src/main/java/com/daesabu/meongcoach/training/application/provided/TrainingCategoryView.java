package com.daesabu.meongcoach.training.application.provided;

import java.util.List;

/**
 * 교육 카테고리 조회 결과. 소속 토픽을 정렬 순서대로 담는다.
 */
public record TrainingCategoryView(
		Long id,
		String title,
		String description,
		String iconUrl,
		int sortOrder,
		List<TopicView> topics
) {
}
