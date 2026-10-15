package com.daesabu.meongcoach.plan.application;

import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.dog.application.provided.DogProfileResult;
import com.daesabu.meongcoach.plan.application.provided.PlanCreator;
import com.daesabu.meongcoach.plan.application.provided.PlanFinder;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanCreateRequest;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanDetailResult;
import com.daesabu.meongcoach.plan.application.required.PlanGenerator;
import com.daesabu.meongcoach.plan.application.required.TrainingPlanRepository;
import com.daesabu.meongcoach.plan.domain.PlanDraft;
import com.daesabu.meongcoach.plan.domain.PlanGeneratorType;
import com.daesabu.meongcoach.plan.domain.PlanMode;
import com.daesabu.meongcoach.plan.domain.PlanStatus;
import com.daesabu.meongcoach.plan.domain.TrainingPlan;
import com.daesabu.meongcoach.plan.domain.exception.PlanAlreadyActiveException;
import com.daesabu.meongcoach.plan.domain.exception.PlanGenerationFailedException;
import com.daesabu.meongcoach.progress.application.provided.LessonProgressFinder;
import com.daesabu.meongcoach.training.application.provided.TrainingCatalogFinder;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogCurriculum;
import com.daesabu.meongcoach.user.application.provided.UserProfileFinder;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
@Validated
public class PlanGenerateService implements PlanCreator {

	private static final int AI_MIN_OPEN_CURRICULUMS = 2;
	private static final int AI_DURATION_WEEKS = 2;

	private final PlanValidator planValidator;
	private final FallbackPlanBuilder fallbackPlanBuilder;
	private final PlanGenerator planGenerator;
	private final PlanStoreService planStoreService;
	private final PlanFinder planFinder;
	private final TrainingPlanRepository trainingPlanRepository;
	private final DogProfileFinder dogProfileFinder;
	private final TrainingCatalogFinder trainingCatalogFinder;
	private final LessonProgressFinder lessonProgressFinder;
	private final UserProfileFinder userProfileFinder;
	private final ObjectMapper objectMapper;
	private final Clock clock;
	private final PlanGenerationProperties generationProperties;

	@Override
	public PlanDetailResult create(Long userId, PlanCreateRequest request) {
		PlanMode mode = PlanMode.from(request.mode());
		planValidator.validateRequest(mode, request);
		validateNoActivePlan(userId, request);
		PlanGenerationContext context = createContext(userId, request);
		GeneratedPlan result = generate(mode, context, request);
		String interviewJson = objectMapper.writeValueAsString(request);
		planStoreService.store(userId, mode, request, result, interviewJson);
		return planFinder.findCurrent(userId).plan();
	}

	private void validateNoActivePlan(Long userId, PlanCreateRequest request) {
		if (request.replace()) {
			return;
		}
		List<TrainingPlan> activePlans = trainingPlanRepository.findByUserIdAndStatus(userId, PlanStatus.ACTIVE);
		if (activePlans.isEmpty()) {
			return;
		}
		Long activeDogId = activePlans.getFirst().getDogId();
		if (dogProfileFinder.findProfileIfPresent(userId, activeDogId).isPresent()) {
			throw new PlanAlreadyActiveException();
		}
	}

	private PlanGenerationContext createContext(Long userId, PlanCreateRequest request) {
		DogProfileResult dog = dogProfileFinder.findProfile(userId, request.dogId());
		TrainingCatalogResult catalog = trainingCatalogFinder.findCatalog(userId);
		Set<Long> completedLessonIds = lessonProgressFinder.findCompletedLessonIds(userId, catalog.allLessonIds());
		Set<Long> priorTopicIds = userProfileFinder.findPriorTrainingTopicIds(userId);
		List<String> priorTopicTitles = priorTopicTitles(catalog, priorTopicIds);
		return new PlanGenerationContext(dog, request.expectation(), request.dailyMinutes(), request.daysPerWeek(),
				priorTopicTitles, completedLessonIds, catalog, LocalDate.now(clock));
	}

	private List<String> priorTopicTitles(TrainingCatalogResult catalog, Set<Long> priorTopicIds) {
		return catalog.categories().stream()
				.flatMap(category -> category.topics().stream())
				.filter(topic -> priorTopicIds.contains(topic.id()))
				.map(TrainingCatalogResult.CatalogTopic::title)
				.toList();
	}

	private GeneratedPlan generate(PlanMode mode, PlanGenerationContext context, PlanCreateRequest request) {
		if (mode == PlanMode.MANUAL) {
			return fallbackPlanBuilder.buildManual(context, request.topicIds());
		}
		return generateWithAi(context);
	}

	private GeneratedPlan generateWithAi(PlanGenerationContext context) {
		if (context.catalog().openCurriculums().size() < AI_MIN_OPEN_CURRICULUMS) {
			return fallbackPlanBuilder.buildFallback(context);
		}
		return requestAiPlan(context).orElseGet(() -> fallbackPlanBuilder.buildFallback(context));
	}

	private Optional<GeneratedPlan> requestAiPlan(PlanGenerationContext context) {
		Set<Long> openCurriculumIds = context.catalog().openCurriculums().stream()
				.map(CatalogCurriculum::id)
				.collect(Collectors.toSet());
		Instant startedAt = clock.instant();
		try {
			PlanDraft draft = planGenerator.generate(context, "");
			List<String> violations = planValidator.validateDraft(draft, openCurriculumIds);
			if (violations.isEmpty()) {
				GeneratedPlan plan = aiPlan(draft);
				return Optional.of(plan);
			}
			return retryAiPlan(context, openCurriculumIds, violations, startedAt);
		} catch (PlanGenerationFailedException e) {
			log.warn("AI 플랜 생성에 실패해 대체 플랜으로 넘어갑니다", e);
			return Optional.empty();
		}
	}

	private Optional<GeneratedPlan> retryAiPlan(PlanGenerationContext context, Set<Long> openCurriculumIds,
			List<String> violations, Instant startedAt) {
		Duration remaining = generationProperties.totalBudget().minus(Duration.between(startedAt, clock.instant()));
		if (remaining.compareTo(generationProperties.callTimeout()) < 0) {
			log.warn("AI 플랜 생성 예산이 부족해 재시도 없이 대체 플랜으로 넘어갑니다. 남은 예산={}", remaining);
			return Optional.empty();
		}
		PlanDraft retried = planGenerator.generate(context, String.join("; ", violations));
		List<String> retriedViolations = planValidator.validateDraft(retried, openCurriculumIds);
		if (retriedViolations.isEmpty()) {
			GeneratedPlan plan = aiPlan(retried);
			return Optional.of(plan);
		}
		log.warn("AI 플랜이 재시도 후에도 검증을 통과하지 못해 대체 플랜으로 넘어갑니다: {}", retriedViolations);
		return Optional.empty();
	}

	private GeneratedPlan aiPlan(PlanDraft draft) {
		return new GeneratedPlan(draft, AI_DURATION_WEEKS, null, PlanGeneratorType.AI);
	}
}
