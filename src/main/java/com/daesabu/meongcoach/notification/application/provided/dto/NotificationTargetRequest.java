package com.daesabu.meongcoach.notification.application.provided.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record NotificationTargetRequest(@NotBlank String kind, List<Long> userIds) {
}
