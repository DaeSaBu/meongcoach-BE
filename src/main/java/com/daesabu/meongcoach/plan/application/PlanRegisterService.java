package com.daesabu.meongcoach.plan.application;

import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.dog.application.provided.dto.DogProfileResult;
import com.daesabu.meongcoach.plan.application.provided.PlanRegister;
import com.daesabu.meongcoach.plan.application.provided.dto.AiPlanCreateRequest;
import com.daesabu.meongcoach.plan.application.provided.dto.CustomPlanCreateRequest;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanOutcomeRecordRequest;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanOutcomeResult;
import com.daesabu.meongcoach.plan.application.required.AiPlanGenerator;
import com.daesabu.meongcoach.plan.application.required.PlanRepository;
import com.daesabu.meongcoach.plan.application.required.dto.AiPlanGenerateRequest;
import com.daesabu.meongcoach.plan.application.required.dto.AiPlanGenerateResult;
import com.daesabu.meongcoach.plan.domain.AiPlanCreateCommand;
import com.daesabu.meongcoach.plan.domain.CustomPlanCreateCommand;
import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.plan.domain.PlanOutcome;
import com.daesabu.meongcoach.plan.domain.exception.EmptyPlanCurriculumsException;
import com.daesabu.meongcoach.plan.domain.exception.PlanNotFoundException;
import com.daesabu.meongcoach.training.application.provided.AiPlanPreparationFinder;
import com.daesabu.meongcoach.training.application.provided.PlanCurriculumFinder;
import com.daesabu.meongcoach.training.application.provided.PlanPreparationFinder;
import com.daesabu.meongcoach.training.application.provided.dto.AiPlanPreparationResult;
import com.daesabu.meongcoach.training.application.provided.dto.PlanPreparationResult;
import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
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
	private final PlanRepository planRepository;
	private final PlanCurriculumFinder planCurriculumFinder;

	@Override
	@Transactional
	public Plan createCustom(Long userId, Long dogId, CustomPlanCreateRequest request) {
		DogProfileResult dog = dogProfileFinder.findDogProfile(userId, dogId);
		PlanPreparationResult preparation = preparationFinder.find(request.topicIds());
		CustomPlanCreateCommand command = new CustomPlanCreateCommand(userId, dog.id(), dog.name());
		Plan plan = Plan.createCustom(command, preparation.curriculumIds());
		return planSaveService.save(plan);
	}

	@Override
	public Plan createAiPlan(Long userId, Long dogId, AiPlanCreateRequest request) {
		DogProfileResult dog = dogProfileFinder.findDogProfile(userId, dogId);
		AiPlanPreparationResult candidates = aiPreparationFinder.find(userId);
		if (candidates.curriculums().isEmpty()) {
			throw new EmptyPlanCurriculumsException();
		}

		List<Long> selectedTopicIds = prepareSelectedTopicIds(request.topicIds());
		LocalDate generationDate = LocalDate.now(GENERATION_ZONE);
		AiPlanGenerateRequest generateRequest = new AiPlanGenerateRequest(dog, generationDate,
				request.expectation(), candidates.curriculums(), selectedTopicIds);
		AiPlanGenerateResult generated = aiPlanGenerator.generate(generateRequest);
		PlanPreparationResult preparation = preparationFinder.findInRequestOrder(generated.topicIds());
		AiPlanCreateCommand command = new AiPlanCreateCommand(userId, dog.id(), dog.name(), generated.goal(),
				request.expectation());
		Plan plan = Plan.createAiPlan(command, preparation.curriculumIds());
		return planSaveService.save(plan);
	}

	private List<Long> prepareSelectedTopicIds(List<Long> topicIds) {
		if (topicIds == null) {
			return List.of();
		}
		preparationFinder.validate(topicIds);
		return topicIds;
	}

	@Override
	@Transactional
	public PlanOutcomeResult recordOutcome(Long userId, Long planId, PlanOutcomeRecordRequest request) {
		Plan plan = planRepository.findByIdAndUserId(planId, userId)
				.orElseThrow(PlanNotFoundException::new);
		PlanOutcome outcome = PlanOutcome.from(request.outcome());
		plan.recordOutcome(outcome);

		int elapsedDays = plan.countElapsedDays(GENERATION_ZONE);
		List<TopicResult> topics = planCurriculumFinder.findTopics(plan.getCurriculumIds());
		return new PlanOutcomeResult(plan, elapsedDays, topics);
	}
}
