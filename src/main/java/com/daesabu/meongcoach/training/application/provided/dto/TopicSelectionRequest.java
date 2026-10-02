package com.daesabu.meongcoach.training.application.provided.dto;

import jakarta.validation.constraints.NotNull;

public record TopicSelectionRequest(@NotNull Long topicId) {
}
