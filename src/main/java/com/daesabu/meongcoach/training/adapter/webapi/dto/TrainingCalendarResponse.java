package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.dto.TrainingCalendarResult;
import java.time.LocalDate;
import java.util.List;

public record TrainingCalendarResponse(LocalDate firstTrainedDate, List<TrainingCalendarDayResponse> days) {

	public static TrainingCalendarResponse from(TrainingCalendarResult result) {
		List<TrainingCalendarDayResponse> days = result.days().stream()
				.map(TrainingCalendarDayResponse::from)
				.toList();
		return new TrainingCalendarResponse(result.firstTrainedDate(), days);
	}
}
