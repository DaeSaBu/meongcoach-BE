package com.daesabu.meongcoach.plan.application;

import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.dog.application.provided.DogProfileResult;
import com.daesabu.meongcoach.plan.application.provided.PlanFinder;
import com.daesabu.meongcoach.plan.application.provided.dto.CurrentPlanResult;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanDetailResult;
import com.daesabu.meongcoach.plan.application.required.TrainingPlanRepository;
import com.daesabu.meongcoach.plan.domain.PlanStatus;
import com.daesabu.meongcoach.plan.domain.TrainingPlan;
import com.daesabu.meongcoach.progress.application.provided.LessonProgressFinder;
import com.daesabu.meongcoach.training.application.provided.TrainingCatalogFinder;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanQueryService implements PlanFinder {

	private final TrainingPlanRepository trainingPlanRepository;
	private final DogProfileFinder dogProfileFinder;
	private final TrainingCatalogFinder trainingCatalogFinder;
	private final LessonProgressFinder lessonProgressFinder;

	@Override
	public CurrentPlanResult findCurrent(Long userId) {
		boolean hasCompletedPlan = trainingPlanRepository.existsByUserIdAndStatus(userId, PlanStatus.COMPLETED);
		List<TrainingPlan> activePlans = trainingPlanRepository.findByUserIdAndStatus(userId, PlanStatus.ACTIVE);
		if (activePlans.isEmpty()) {
			return new CurrentPlanResult(hasCompletedPlan, null);
		}
		PlanDetailResult detail = assemble(userId, activePlans.getFirst());
		return new CurrentPlanResult(hasCompletedPlan, detail);
	}

	private PlanDetailResult assemble(Long userId, TrainingPlan plan) {
		DogProfileResult dog = dogProfileFinder.findProfile(userId, plan.getDogId());
		TrainingCatalogResult catalog = trainingCatalogFinder.findCatalog(userId);
		Set<Long> completedLessonIds = lessonProgressFinder.findCompletedLessonIds(userId, catalog.allLessonIds());
		return PlanDetailAssembler.assemble(plan, dog.name(), catalog, completedLessonIds, LocalDate.now());
	}
}
