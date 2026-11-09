package com.daesabu.meongcoach.training.application.provided.dto;

import java.util.List;

public record PlanCurriculumResult(Long id, Long topicId, String topicTitle, String title, String description,
		boolean locked, List<PlanLessonResult> lessons) {
}
