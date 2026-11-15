package com.daesabu.meongcoach.training.application.required.dto;

import java.util.List;

public record AiPlanGenerateResult(List<Long> topicIds, String goal) {
}
