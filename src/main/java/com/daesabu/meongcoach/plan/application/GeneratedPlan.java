package com.daesabu.meongcoach.plan.application;

import com.daesabu.meongcoach.plan.domain.PlanAdjustedReason;
import com.daesabu.meongcoach.plan.domain.PlanDraft;
import com.daesabu.meongcoach.plan.domain.PlanGeneratorType;

public record GeneratedPlan(PlanDraft draft, int durationWeeks, PlanAdjustedReason adjustedReason,
		PlanGeneratorType generatorType) {
}
