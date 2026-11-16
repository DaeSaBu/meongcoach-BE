package com.daesabu.meongcoach.training.adapter.webapi;

import com.daesabu.meongcoach.shared.security.CurrentUserId;
import com.daesabu.meongcoach.training.adapter.webapi.dto.TrainingCalendarResponse;
import com.daesabu.meongcoach.training.application.provided.CurriculumProgressRegister;
import com.daesabu.meongcoach.training.application.provided.TrainingCalendarFinder;
import com.daesabu.meongcoach.training.application.provided.dto.CurriculumProgressResultRequest;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCalendarResult;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/training/calendar")
@RequiredArgsConstructor
public class TrainingCalendarController {

	private final TrainingCalendarFinder trainingCalendarFinder;
	private final CurriculumProgressRegister curriculumProgressRegister;

	@GetMapping
	public TrainingCalendarResponse find(@CurrentUserId Long userId, @RequestParam Long dogId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		TrainingCalendarResult result = trainingCalendarFinder.find(userId, dogId, from, to);
		return TrainingCalendarResponse.from(result);
	}

	@PutMapping("/{date}/curriculums/{curriculumId}/result")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void recordResult(@CurrentUserId Long userId,
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@PathVariable Long curriculumId, @RequestParam Long dogId,
			@Valid @RequestBody CurriculumProgressResultRequest request) {
		curriculumProgressRegister.recordResult(userId, dogId, curriculumId, date, request);
	}
}
