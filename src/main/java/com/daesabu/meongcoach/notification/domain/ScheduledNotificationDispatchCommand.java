package com.daesabu.meongcoach.notification.domain;

import java.time.LocalDate;

public record ScheduledNotificationDispatchCommand(LocalDate date, int hour, int minute) {
}
