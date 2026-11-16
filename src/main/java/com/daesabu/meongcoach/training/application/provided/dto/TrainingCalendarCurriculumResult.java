package com.daesabu.meongcoach.training.application.provided.dto;

import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumProgressStatus;
import java.util.List;

public record TrainingCalendarCurriculumResult(Curriculum curriculum, CurriculumProgressStatus status,
		List<TrainingCalendarLessonResult> lessons) {
}
