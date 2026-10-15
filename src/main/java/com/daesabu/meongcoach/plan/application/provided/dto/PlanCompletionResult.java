package com.daesabu.meongcoach.plan.application.provided.dto;

import com.daesabu.meongcoach.plan.domain.PlanOutcome;
import java.util.List;

public record PlanCompletionResult(Long planId, String title, PlanOutcome outcome, int completedCurriculums,
		int totalCurriculums, int totalLessons, long daysTaken, List<ReflectedTopic> reflectedTopics) {

	public record ReflectedTopic(Long topicId, String topicTitle) {
	}
}
