package com.daesabu.meongcoach.notification.application.provided.dto;

import jakarta.validation.constraints.NotNull;

public record NotificationPreferenceUpdateRequest(@NotNull Boolean enabled) {
}
