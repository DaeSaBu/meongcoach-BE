package com.daesabu.meongcoach.training.application.required.dto;

import com.daesabu.meongcoach.dog.application.provided.dto.DogProfileResult;
import com.daesabu.meongcoach.training.application.provided.dto.AiPlanCurriculumResult;
import java.time.LocalDate;
import java.util.List;

public record AiPlanGenerateRequest(DogProfileResult dog, LocalDate generationDate, String expectation,
		 List<AiPlanCurriculumResult> curriculums, List<Long> selectedTopicIds) {
	public AiPlanGenerateRequest(DogProfileResult dog, LocalDate generationDate, String expectation,
			List<AiPlanCurriculumResult> curriculums) {
		this(dog, generationDate, expectation, curriculums, List.of());
	}
}
