package com.daesabu.meongcoach.training.application.provided.dto;

import com.daesabu.meongcoach.training.domain.Plan;
import java.util.List;

public record PlanOutcomeResult(Plan plan, int elapsedDays, List<TopicResult> topics) {
}
