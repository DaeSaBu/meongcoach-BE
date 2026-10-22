package com.daesabu.meongcoach.training.application.provided.dto;

import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumCompletion;
import com.daesabu.meongcoach.training.domain.LessonProgresses;

public record CurriculumResult(Curriculum curriculum, CurriculumCompletion completion, boolean locked) {

	public static CurriculumResult of(Curriculum curriculum, LessonProgresses progresses, boolean hasEntitlement) {
		return new CurriculumResult(curriculum, curriculum.completion(progresses), curriculum.isLocked(hasEntitlement));
	}
}
