package com.daesabu.meongcoach.plan.application;

import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.dog.application.provided.dto.DogSummaryResult;
import com.daesabu.meongcoach.plan.application.provided.CustomPlanCreator;
import com.daesabu.meongcoach.plan.application.provided.dto.CustomPlanCreateRequest;
import com.daesabu.meongcoach.plan.application.required.PlanRepository;
import com.daesabu.meongcoach.plan.domain.CustomPlanCreateCommand;
import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.plan.domain.PlanStatus;
import com.daesabu.meongcoach.plan.domain.exception.PlanCreationConflictException;
import com.daesabu.meongcoach.training.application.provided.CustomPlanPreparationFinder;
import com.daesabu.meongcoach.training.application.provided.dto.CustomPlanPreparationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class CustomPlanCreateService implements CustomPlanCreator {

	private final DogProfileFinder dogProfileFinder;
	private final CustomPlanPreparationFinder preparationFinder;
	private final PlanRepository planRepository;

	@Override
	@Transactional
	public Plan create(Long userId, CustomPlanCreateRequest request) {
		DogSummaryResult dog = dogProfileFinder.findSelectedDogSummary(userId);
		CustomPlanPreparationResult preparation = preparationFinder.find(userId, request.topicIds());
		CustomPlanCreateCommand command = new CustomPlanCreateCommand(userId, dog.id(), dog.name(),
				request.dailyMinutes(), request.daysPerWeek());
		Plan plan = Plan.createCustom(command, preparation.curriculumIds());
		planRepository.findByUserIdAndStatus(userId, PlanStatus.ACTIVE).ifPresent(Plan::replace);
		try {
			planRepository.flush();
			return planRepository.saveAndFlush(plan);
		} catch (DataIntegrityViolationException exception) {
			throw new PlanCreationConflictException(exception);
		}
	}
}
