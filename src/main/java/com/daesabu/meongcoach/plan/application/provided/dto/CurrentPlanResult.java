package com.daesabu.meongcoach.plan.application.provided.dto;

import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.training.application.provided.dto.PlanCurriculumResult;
import java.util.List;

public record CurrentPlanResult(Plan plan, List<PlanCurriculumResult> curriculums) {
}
