package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.dto.TrainingCalendarCurriculumResult;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumProgressStatus;
import java.util.List;

public record TrainingCalendarCurriculumResponse(Long curriculumId, String curriculumTitle,
		CurriculumProgressStatus status, List<TrainingCalendarLessonResponse> lessons) {

	public static TrainingCalendarCurriculumResponse from(TrainingCalendarCurriculumResult result) {
		Curriculum curriculum = result.curriculum();
		List<TrainingCalendarLessonResponse> lessons = result.lessons().stream()
				.map(TrainingCalendarLessonResponse::from)
				.toList();
		return new TrainingCalendarCurriculumResponse(curriculum.getId(), curriculum.getTitle(), result.status(),
				lessons);
	}
}
