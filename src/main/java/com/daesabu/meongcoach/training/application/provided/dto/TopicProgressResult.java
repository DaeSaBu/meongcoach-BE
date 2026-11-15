package com.daesabu.meongcoach.training.application.provided.dto;

import com.daesabu.meongcoach.training.domain.ProgressStatus;
import java.time.LocalDateTime;

public record TopicProgressResult(Long id, String title, String description, String iconUrl, String thumbnailUrl,
		boolean hasThumbnail, Long categoryId, String categoryTitle, int categorySortOrder, int totalLessons,
		int completedLessons, ProgressStatus status, Long nextCurriculumId, LocalDateTime lastStudiedAt) {

	public boolean isNotStarted() {
		return status == ProgressStatus.NOT_STARTED;
	}

	public boolean isInProgress() {
		return status == ProgressStatus.IN_PROGRESS;
	}
}
