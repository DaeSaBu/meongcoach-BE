package com.daesabu.meongcoach.training.application.provided.dto;

import java.time.LocalDate;
import java.util.List;

public record TrainingCalendarDayResult(LocalDate trainedDate, List<TrainingCalendarTopicResult> plan,
		List<TrainingCalendarTopicResult> library) {
}
