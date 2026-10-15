package com.daesabu.meongcoach.plan.domain;

import java.util.List;

public record PlanDraft(String title, String goal, String reason, List<WeekDraft> weeks) {

	public record WeekDraft(int weekNo, String theme, String milestone, List<Long> curriculumIds) {
	}
}
