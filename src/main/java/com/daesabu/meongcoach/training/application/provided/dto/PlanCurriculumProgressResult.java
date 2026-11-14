package com.daesabu.meongcoach.training.application.provided.dto;

import com.daesabu.meongcoach.training.domain.ProgressStatus;
import java.util.List;

public record PlanCurriculumProgressResult(Long curriculumId, Long topicId, String topicTitle, String title,
		String description, int totalLessons, int completedLessons, ProgressStatus status, boolean locked,
		List<PlanLessonProgressResult> lessons) {
}
