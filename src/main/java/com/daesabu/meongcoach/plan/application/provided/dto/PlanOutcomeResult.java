package com.daesabu.meongcoach.plan.application.provided.dto;

import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;
import java.util.List;

public record PlanOutcomeResult(Plan plan, int elapsedDays, List<TopicResult> topics) {
}
