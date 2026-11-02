package com.daesabu.meongcoach.notification.application.provided.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record EventNotificationDispatchRequest(
		@NotBlank String type,
		@NotBlank String targetKind,
		List<Long> userIds,
		@NotBlank @Size(max = 100) String title,
		@NotBlank @Size(max = 500) String body,
		String deepLink
) {
}
