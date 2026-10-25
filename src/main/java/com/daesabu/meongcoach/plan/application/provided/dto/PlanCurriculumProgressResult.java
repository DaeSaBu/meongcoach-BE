package com.daesabu.meongcoach.plan.application.provided.dto;

import com.daesabu.meongcoach.training.domain.shared.ProgressStatus;
import java.util.List;

public record PlanCurriculumProgressResult(Long curriculumId, String title, String description, int totalLessons,
		int completedLessons, ProgressStatus status, boolean locked, List<PlanLessonProgressResult> lessons) {
}
