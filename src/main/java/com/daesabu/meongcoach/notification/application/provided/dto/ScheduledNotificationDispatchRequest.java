package com.daesabu.meongcoach.notification.application.provided.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ScheduledNotificationDispatchRequest(@NotNull LocalDate date, @NotNull Integer hour, @NotNull Integer minute) {
}
