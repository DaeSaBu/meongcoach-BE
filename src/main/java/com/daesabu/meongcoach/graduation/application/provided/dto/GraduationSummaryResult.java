package com.daesabu.meongcoach.graduation.application.provided.dto;

import com.daesabu.meongcoach.graduation.domain.TopicGraduation;

public record GraduationSummaryResult(TopicGraduation graduation, String topicTitle) {
}
