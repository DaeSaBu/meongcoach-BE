package com.daesabu.meongcoach.plan.application;

import com.daesabu.meongcoach.dog.application.provided.DogProfileResult;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record PlanGenerationContext(DogProfileResult dog, String expectation, int dailyMinutes, int daysPerWeek,
		List<String> priorTopicTitles, Set<Long> completedLessonIds, TrainingCatalogResult catalog, LocalDate today) {
}
