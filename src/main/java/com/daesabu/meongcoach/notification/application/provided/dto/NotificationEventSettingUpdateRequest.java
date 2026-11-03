package com.daesabu.meongcoach.notification.application.provided.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationEventSettingUpdateRequest(@NotBlank String type, @NotNull Boolean enabled) {
}
