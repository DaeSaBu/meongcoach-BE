package com.daesabu.meongcoach.plan.application.required.dto;

import java.util.List;

public record AiPlanGenerateResult(List<Long> topicIds, String goal) {
}
