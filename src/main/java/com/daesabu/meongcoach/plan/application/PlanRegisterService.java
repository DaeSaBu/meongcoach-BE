package com.daesabu.meongcoach.plan.application;

import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.dog.application.provided.dto.DogProfileResult;
import com.daesabu.meongcoach.dog.application.provided.dto.DogSummaryResult;
import com.daesabu.meongcoach.plan.application.provided.PlanRegister;
import com.daesabu.meongcoach.plan.application.provided.dto.AiPlanCreateRequest;
import com.daesabu.meongcoach.plan.application.provided.dto.CustomPlanCreateRequest;
import com.daesabu.meongcoach.plan.application.required.AiPlanGenerator;
import com.daesabu.meongcoach.plan.application.required.dto.AiPlanGenerateRequest;
import com.daesabu.meongcoach.plan.application.required.dto.AiPlanGenerateResult;
import com.daesabu.meongcoach.plan.domain.AiPlanCreateCommand;
import com.daesabu.meongcoach.plan.domain.CustomPlanCreateCommand;
import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.plan.domain.exception.EmptyPlanCurriculumsException;
import com.daesabu.meongcoach.training.application.provided.AiPlanPreparationFinder;
import com.daesabu.meongcoach.training.application.provided.PlanPreparationFinder;
import com.daesabu.meongcoach.training.application.provided.dto.AiPlanPreparationResult;
import com.daesabu.meongcoach.training.application.provided.dto.PlanPreparationResult;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Validated
public class PlanRegisterService implements PlanRegister {

	private static final ZoneId GENERATION_ZONE = ZoneId.of("Asia/Seoul");

	private final DogProfileFinder dogProfileFinder;
	private final PlanPreparationFinder preparationFinder;
	private final AiPlanPreparationFinder aiPreparationFinder;
	private final AiPlanGenerator aiPlanGenerator;
	private final PlanSaveService planSaveService;

	@Override
	@Transactional
	public Plan createCustom(Long userId, CustomPlanCreateRequest request) {
		DogSummaryResult dog = dogProfileFinder.findSelectedDogSummary(userId);
		PlanPreparationResult preparation = preparationFinder.find(userId, request.topicIds());
		CustomPlanCreateCommand command = new CustomPlanCreateCommand(userId, dog.id(), dog.name(),
				request.dailyMinutes(), request.daysPerWeek());
		Plan plan = Plan.createCustom(command, preparation.curriculumIds());
		return planSaveService.save(plan);
	}

	@Override
	public Plan createAi(Long userId, AiPlanCreateRequest request) {
		Plan.validatePracticeSettings(request.dailyMinutes(), request.daysPerWeek());
		DogProfileResult dog = dogProfileFinder.findSelectedDogProfile(userId);
		AiPlanPreparationResult candidates = aiPreparationFinder.find(userId);
		boolean hasIncompleteCurriculum = candidates.curriculums().stream()
				.anyMatch(curriculum -> curriculum.completedLessons() < curriculum.totalLessons());
		if (!hasIncompleteCurriculum) {
			throw new EmptyPlanCurriculumsException();
		}

		LocalDate generationDate = LocalDate.now(GENERATION_ZONE);
		AiPlanGenerateRequest generateRequest = new AiPlanGenerateRequest(dog, generationDate,
				request.dailyMinutes(), request.daysPerWeek(), request.expectation(), candidates.curriculums());
		AiPlanGenerateResult generated = aiPlanGenerator.generate(generateRequest);
		PlanPreparationResult preparation = preparationFinder.find(userId, generated.topicIds());
		AiPlanCreateCommand command = new AiPlanCreateCommand(userId, dog.id(), dog.name(), generated.goal(),
				request.dailyMinutes(), request.daysPerWeek(), request.expectation());
		Plan plan = Plan.createAi(command, preparation.curriculumIds());
		return planSaveService.save(plan);
	}
}
