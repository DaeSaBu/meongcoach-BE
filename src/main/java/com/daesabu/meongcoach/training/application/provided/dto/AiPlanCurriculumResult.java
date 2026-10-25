package com.daesabu.meongcoach.training.application.provided.dto;

public record AiPlanCurriculumResult(Long id, String topicTitle, String title, String description,
		int totalLessons, int completedLessons, int estimatedMinutes) {
}
