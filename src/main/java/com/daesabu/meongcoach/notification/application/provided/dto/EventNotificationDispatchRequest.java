package com.daesabu.meongcoach.notification.application.provided.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EventNotificationDispatchRequest(
		@NotBlank String type,
		@NotNull @Valid NotificationTargetRequest target,
		@NotBlank @Size(max = 100) String title,
		@NotBlank @Size(max = 500) String body,
		String deepLink
) {
}
