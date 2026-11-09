package com.daesabu.meongcoach.home.adapter.webapi.dto;

import com.daesabu.meongcoach.home.application.provided.dto.QuickTopicResult;
import com.daesabu.meongcoach.home.domain.QuickTopicRole;
import com.daesabu.meongcoach.training.application.provided.dto.TopicProgressResult;

public record QuickTopicResponse(Long topicId, String title, String description, String categoryTitle, String iconUrl,
		String thumbnailUrl, QuickTopicRole role, int totalLessons, int completedLessons, Long nextCurriculumId) {

	public static QuickTopicResponse from(QuickTopicResult result) {
		TopicProgressResult topic = result.topic();
		return new QuickTopicResponse(topic.id(), topic.title(), topic.description(), topic.categoryTitle(),
				topic.iconUrl(), topic.thumbnailUrl(), result.role(), topic.totalLessons(), topic.completedLessons(),
				topic.nextCurriculumId());
	}
}
