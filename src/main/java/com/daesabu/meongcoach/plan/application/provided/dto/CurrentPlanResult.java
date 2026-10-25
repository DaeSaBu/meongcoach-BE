package com.daesabu.meongcoach.plan.application.provided.dto;

import com.daesabu.meongcoach.plan.domain.Plan;
import java.util.List;

public record CurrentPlanResult(Plan plan, List<PlanCurriculumProgressResult> curriculums) {
}
