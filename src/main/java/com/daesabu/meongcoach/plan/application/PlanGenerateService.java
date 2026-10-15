package com.daesabu.meongcoach.plan.application;

import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.dog.application.provided.DogProfileResult;
import com.daesabu.meongcoach.plan.application.FallbackPlanBuilder.FallbackResult;
import com.daesabu.meongcoach.plan.application.provided.PlanCreator;
import com.daesabu.meongcoach.plan.application.provided.PlanFinder;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanCreateRequest;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanDetailResult;
import com.daesabu.meongcoach.plan.application.required.TrainingPlanRepository;
import com.daesabu.meongcoach.plan.domain.PlanMode;
import com.daesabu.meongcoach.plan.domain.PlanStatus;
import com.daesabu.meongcoach.plan.domain.exception.PlanAlreadyActiveException;
import com.daesabu.meongcoach.progress.application.provided.LessonProgressFinder;
import com.daesabu.meongcoach.training.application.provided.TrainingCatalogFinder;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult;
import com.daesabu.meongcoach.user.application.provided.UserProfileFinder;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Validated
public class PlanGenerateService implements PlanCreator {

	private final PlanValidator planValidator;
	private final FallbackPlanBuilder fallbackPlanBuilder;
	private final PlanStoreService planStoreService;
	private final PlanFinder planFinder;
	private final TrainingPlanRepository trainingPlanRepository;
	private final DogProfileFinder dogProfileFinder;
	private final TrainingCatalogFinder trainingCatalogFinder;
	private final LessonProgressFinder lessonProgressFinder;
	private final UserProfileFinder userProfileFinder;
	private final ObjectMapper objectMapper;

	@Override
	public PlanDetailResult create(Long userId, PlanCreateRequest request) {
		PlanMode mode = PlanMode.from(request.mode());
		planValidator.validateRequest(mode, request);
		validateNoActivePlan(userId, request);
		PlanGenerationContext context = createContext(userId, request);
		FallbackResult result = generate(mode, context, request);
		String interviewJson = objectMapper.writeValueAsString(request);
		planStoreService.store(userId, mode, request, result, interviewJson);
		return planFinder.findCurrent(userId).plan();
	}

	private void validateNoActivePlan(Long userId, PlanCreateRequest request) {
		if (request.replace()) {
			return;
		}
		if (trainingPlanRepository.existsByUserIdAndStatus(userId, PlanStatus.ACTIVE)) {
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
				priorTopicTitles, completedLessonIds, catalog, LocalDate.now());
	}

	private List<String> priorTopicTitles(TrainingCatalogResult catalog, Set<Long> priorTopicIds) {
		return catalog.categories().stream()
				.flatMap(category -> category.topics().stream())
				.filter(topic -> priorTopicIds.contains(topic.id()))
				.map(TrainingCatalogResult.CatalogTopic::title)
				.toList();
	}

	private FallbackResult generate(PlanMode mode, PlanGenerationContext context, PlanCreateRequest request) {
		if (mode == PlanMode.MANUAL) {
			return fallbackPlanBuilder.buildManual(context, request.topicIds());
		}
		return fallbackPlanBuilder.buildFallback(context);
	}
}
