package com.daesabu.meongcoach.plan.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;

public record PlanTopicResponse(Long topicId, String title) {

	public static PlanTopicResponse from(TopicResult topic) {
		return new PlanTopicResponse(topic.id(), topic.title());
	}
}
