package com.daesabu.meongcoach.training.application.provided.dto;

import java.time.LocalDateTime;

public record TopicProgressResult(Long id, String title, String description, String iconUrl, String thumbnailUrl,
		Long categoryId, String categoryTitle, int totalLessons, int completedLessons, Long nextCurriculumId,
		LocalDateTime lastStudiedAt) {

	public boolean isStarted() {
		return completedLessons > 0;
	}

	public boolean isInProgress() {
		return isStarted() && completedLessons < totalLessons;
	}
}
