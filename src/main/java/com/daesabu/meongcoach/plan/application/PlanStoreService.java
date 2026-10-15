package com.daesabu.meongcoach.plan.application;

import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.dog.application.provided.DogProfileUpdater;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanCreateRequest;
import com.daesabu.meongcoach.plan.application.required.TrainingPlanRepository;
import com.daesabu.meongcoach.plan.domain.PlanMode;
import com.daesabu.meongcoach.plan.domain.PlanStatus;
import com.daesabu.meongcoach.plan.domain.TrainingPlan;
import com.daesabu.meongcoach.plan.domain.TrainingPlanCreateCommand;
import com.daesabu.meongcoach.plan.domain.exception.PlanAlreadyActiveException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanStoreService {

	private static final String ACTIVE_PLAN_CONSTRAINT = "uk_training_plans_user_id_active";

	private final TrainingPlanRepository trainingPlanRepository;
	private final DogProfileFinder dogProfileFinder;
	private final DogProfileUpdater dogProfileUpdater;
	private final Clock clock;

	@Transactional
	public void store(Long userId, PlanMode mode, PlanCreateRequest request, GeneratedPlan result,
			String interviewJson) {
		abandonActivePlans(userId, request.replace());
		TrainingPlanCreateCommand command = new TrainingPlanCreateCommand(userId, request.dogId(), mode,
				result.generatorType(), result.adjustedReason(), result.durationWeeks(), request.dailyMinutes(),
				request.daysPerWeek(), LocalDate.now(clock), interviewJson, result.draft());
		saveOrConflict(TrainingPlan.create(command));
		updateExpectation(userId, request);
	}

	private void saveOrConflict(TrainingPlan plan) {
		try {
			trainingPlanRepository.saveAndFlush(plan);
		} catch (DataIntegrityViolationException e) {
			if (violatesActivePlanConstraint(e)) {
				throw new PlanAlreadyActiveException();
			}
			throw e;
		}
	}

	private boolean violatesActivePlanConstraint(DataIntegrityViolationException e) {
		String message = e.getMostSpecificCause().getMessage();
		return message != null && message.contains(ACTIVE_PLAN_CONSTRAINT);
	}

	private void abandonActivePlans(Long userId, boolean replace) {
		List<TrainingPlan> activePlans = trainingPlanRepository.findByUserIdAndStatus(userId, PlanStatus.ACTIVE);
		if (activePlans.isEmpty()) {
			return;
		}
		TrainingPlan active = activePlans.getFirst();
		if (!replace && hasDog(userId, active)) {
			throw new PlanAlreadyActiveException();
		}
		active.abandon();
		trainingPlanRepository.flush();
	}

	private boolean hasDog(Long userId, TrainingPlan plan) {
		return dogProfileFinder.findProfileIfPresent(userId, plan.getDogId()).isPresent();
	}

	private void updateExpectation(Long userId, PlanCreateRequest request) {
		String expectation = request.expectation();
		if (expectation == null || expectation.isBlank()) {
			return;
		}
		dogProfileUpdater.updateExpectation(userId, request.dogId(), expectation.strip());
	}
}
