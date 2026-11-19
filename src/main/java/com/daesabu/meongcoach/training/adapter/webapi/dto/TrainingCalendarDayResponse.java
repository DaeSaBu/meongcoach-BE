package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.dto.TrainingCalendarDayResult;
import java.time.LocalDate;
import java.util.List;

public record TrainingCalendarDayResponse(LocalDate date, List<TrainingCalendarTopicResponse> plan,
		List<TrainingCalendarTopicResponse> library) {

	public static TrainingCalendarDayResponse from(TrainingCalendarDayResult result) {
		List<TrainingCalendarTopicResponse> plan = result.plan().stream()
				.map(TrainingCalendarTopicResponse::from)
				.toList();
		List<TrainingCalendarTopicResponse> library = result.library().stream()
				.map(TrainingCalendarTopicResponse::from)
				.toList();
		return new TrainingCalendarDayResponse(result.trainedDate(), plan, library);
	}
}
