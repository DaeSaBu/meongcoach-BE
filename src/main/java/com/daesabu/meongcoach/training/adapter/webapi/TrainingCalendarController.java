package com.daesabu.meongcoach.training.adapter.webapi;

import com.daesabu.meongcoach.shared.security.CurrentUserId;
import com.daesabu.meongcoach.training.adapter.webapi.dto.TrainingCalendarResponse;
import com.daesabu.meongcoach.training.application.provided.TrainingCalendarFinder;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCalendarResult;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/training/calendar")
@RequiredArgsConstructor
public class TrainingCalendarController {

	private final TrainingCalendarFinder trainingCalendarFinder;

	@GetMapping
	public TrainingCalendarResponse find(@CurrentUserId Long userId, @RequestParam Long dogId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		TrainingCalendarResult result = trainingCalendarFinder.find(userId, dogId, from, to);
		return TrainingCalendarResponse.from(result);
	}
}
