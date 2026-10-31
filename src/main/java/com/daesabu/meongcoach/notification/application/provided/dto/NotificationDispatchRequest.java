package com.daesabu.meongcoach.notification.application.provided.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record NotificationDispatchRequest(@NotNull Instant scheduledTime, @NotNull Integer hour) {
	public String key() {
		return scheduledTime.toString() + ":" + hour;
	}
}
