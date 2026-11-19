package com.daesabu.meongcoach.training.domain;

import static java.util.Objects.requireNonNull;

import com.daesabu.meongcoach.training.domain.exception.InvalidTrainingCalendarPeriodException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record TrainingCalendarPeriod(LocalDate from, LocalDate to) {

	public static final int MAX_DAYS = 62;

	public TrainingCalendarPeriod {
		requireNonNull(from);
		requireNonNull(to);
		if (from.isAfter(to)) {
			throw new InvalidTrainingCalendarPeriodException();
		}
		if (ChronoUnit.DAYS.between(from, to) >= MAX_DAYS) {
			throw new InvalidTrainingCalendarPeriodException();
		}
	}

	public boolean contains(LocalDate date) {
		return !date.isBefore(from) && !date.isAfter(to);
	}
}
