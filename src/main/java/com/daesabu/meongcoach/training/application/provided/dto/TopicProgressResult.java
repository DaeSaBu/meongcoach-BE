package com.daesabu.meongcoach.training.application.provided.dto;

import com.daesabu.meongcoach.training.domain.shared.ProgressStatus;
import java.time.LocalDateTime;

public record TopicProgressResult(Long id, String title, String description, String iconUrl, String thumbnailUrl,
		boolean hasThumbnail, Long categoryId, String categoryTitle, int categorySortOrder, int totalLessons,
		int completedLessons, ProgressStatus status, Long nextCurriculumId, LocalDateTime lastStudiedAt) {
}
