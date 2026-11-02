package com.daesabu.meongcoach.notification.application.provided.dto;

import jakarta.validation.constraints.NotNull;

public record NotificationScheduleUpdateRequest(@NotNull Boolean enabled) {
}
