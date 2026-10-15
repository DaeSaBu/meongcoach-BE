package com.daesabu.meongcoach.plan.adapter.webapi.dto;

import com.daesabu.meongcoach.plan.application.provided.dto.PlanCompletionResult;
import com.daesabu.meongcoach.plan.domain.PlanOutcome;
import java.util.List;

public record PlanCompletionResponse(Long planId, String title, PlanOutcome outcome, int completedCurriculums,
		int totalCurriculums, int totalLessons, long daysTaken, List<ReflectedTopicResponse> reflectedTopics) {

	public static PlanCompletionResponse from(PlanCompletionResult result) {
		List<ReflectedTopicResponse> reflectedTopics = result.reflectedTopics().stream()
				.map(ReflectedTopicResponse::from)
				.toList();
		return new PlanCompletionResponse(result.planId(), result.title(), result.outcome(),
				result.completedCurriculums(), result.totalCurriculums(), result.totalLessons(), result.daysTaken(),
				reflectedTopics);
	}

	public record ReflectedTopicResponse(Long topicId, String topicTitle) {

		static ReflectedTopicResponse from(PlanCompletionResult.ReflectedTopic topic) {
			return new ReflectedTopicResponse(topic.topicId(), topic.topicTitle());
		}
	}
}
