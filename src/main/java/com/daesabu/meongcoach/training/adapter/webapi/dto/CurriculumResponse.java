package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.dto.CurriculumResult;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumCompletion;
import com.daesabu.meongcoach.training.domain.ProgressStatus;

public record CurriculumResponse(Long curriculumId, String curriculumTitle, int totalLessons, int completedLessons,
		ProgressStatus status, boolean locked) {

	public static CurriculumResponse from(CurriculumResult result) {
		Curriculum curriculum = result.curriculum();
		CurriculumCompletion completion = result.completion();
		return new CurriculumResponse(curriculum.getId(), curriculum.getTitle(), curriculum.getLessonsSize(),
				completion.completedLessons(), completion.status(), result.locked());
	}
}
