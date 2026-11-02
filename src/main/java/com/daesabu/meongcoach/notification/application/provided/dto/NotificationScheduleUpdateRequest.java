package com.daesabu.meongcoach.notification.application.provided.dto;

import jakarta.validation.constraints.NotNull;

public record NotificationScheduleUpdateRequest(@NotNull Integer hour, @NotNull Integer minute, @NotNull Boolean enabled) {
}
