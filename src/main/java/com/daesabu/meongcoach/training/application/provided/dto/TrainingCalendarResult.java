package com.daesabu.meongcoach.training.application.provided.dto;

import java.time.LocalDate;
import java.util.List;

public record TrainingCalendarResult(LocalDate firstTrainedDate, List<TrainingCalendarDayResult> days) {
}
