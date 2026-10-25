package com.daesabu.meongcoach.plan.application.required.dto;

import com.daesabu.meongcoach.dog.application.provided.dto.DogProfileResult;
import com.daesabu.meongcoach.training.application.provided.dto.AiPlanCurriculumResult;
import java.time.LocalDate;
import java.util.List;

public record AiPlanGenerateRequest(DogProfileResult dog, LocalDate generationDate, int dailyMinutes,
		int daysPerWeek, String expectation, List<AiPlanCurriculumResult> curriculums) {
}
