package com.daesabu.meongcoach.notification.application.provided.dto;

import com.daesabu.meongcoach.notification.domain.ScheduledNotificationDispatchCommand;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ScheduledNotificationDispatchRequest(@NotNull LocalDate date, @NotNull Integer hour, @NotNull Integer minute) {

	public ScheduledNotificationDispatchCommand toCommand() {
		return new ScheduledNotificationDispatchCommand(date, hour, minute);
	}
}
