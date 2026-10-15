package com.daesabu.meongcoach.plan.application;

import com.daesabu.meongcoach.plan.application.provided.PlanCompleter;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanCompleteRequest;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanCompletionResult;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanCompletionResult.ReflectedTopic;
import com.daesabu.meongcoach.plan.application.required.TrainingPlanRepository;
import com.daesabu.meongcoach.plan.domain.PlanOutcome;
import com.daesabu.meongcoach.plan.domain.TrainingPlan;
import com.daesabu.meongcoach.plan.domain.exception.PlanNotFoundException;
import com.daesabu.meongcoach.progress.application.provided.LessonProgressFinder;
import com.daesabu.meongcoach.training.application.provided.TrainingCatalogFinder;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogEntry;
import com.daesabu.meongcoach.training.domain.shared.CurriculumStatus;
import com.daesabu.meongcoach.user.application.provided.UserProfileUpdater;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Validated
@Transactional(readOnly = true)
public class PlanCompleteService implements PlanCompleter {

	private final TrainingPlanRepository trainingPlanRepository;
	private final TrainingCatalogFinder trainingCatalogFinder;
	private final LessonProgressFinder lessonProgressFinder;
	private final UserProfileUpdater userProfileUpdater;
	private final Clock clock;

	@Override
	@Transactional
	public PlanCompletionResult complete(Long userId, Long planId, PlanCompleteRequest request) {
		PlanOutcome outcome = PlanOutcome.from(request.outcome());
		TrainingPlan plan = trainingPlanRepository.findByIdAndUserId(planId, userId)
				.orElseThrow(PlanNotFoundException::new);
		plan.complete(outcome, LocalDateTime.now(clock));
		TrainingCatalogResult catalog = trainingCatalogFinder.findCatalog(userId);
		Set<Long> completedLessonIds = lessonProgressFinder.findCompletedLessonIds(userId, catalog.allLessonIds());
		List<CatalogEntry> entries = plan.curriculumIds().stream()
				.map(catalog::findCurriculum)
				.flatMap(Optional::stream)
				.toList();
		List<ReflectedTopic> planTopics = planTopics(catalog, entries);
		List<ReflectedTopic> reflectedTopics = reflect(userId, outcome, planTopics);
		int completedCurriculums = (int) entries.stream()
				.filter(entry -> isCompleted(entry, completedLessonIds))
				.count();
		int totalLessons = entries.stream()
				.mapToInt(entry -> entry.curriculum().lessons().size())
				.sum();
		long daysTaken = ChronoUnit.DAYS.between(plan.getStartedOn(), LocalDate.now(clock)) + 1;
		return new PlanCompletionResult(plan.getId(), plan.getTitle(), outcome, completedCurriculums, entries.size(),
				totalLessons, daysTaken, reflectedTopics);
	}

	private List<ReflectedTopic> reflect(Long userId, PlanOutcome outcome, List<ReflectedTopic> planTopics) {
		if (!outcome.reflectsToProfile()) {
			return List.of();
		}
		List<Long> topicIds = planTopics.stream()
				.map(ReflectedTopic::topicId)
				.toList();
		userProfileUpdater.addPriorTrainingTopics(userId, topicIds);
		return planTopics;
	}

	private List<ReflectedTopic> planTopics(TrainingCatalogResult catalog, List<CatalogEntry> entries) {
		Set<Long> planTopicIds = entries.stream()
				.map(entry -> entry.topic().id())
				.collect(Collectors.toSet());
		return catalog.categories().stream()
				.flatMap(category -> category.topics().stream())
				.filter(topic -> planTopicIds.contains(topic.id()))
				.map(topic -> new ReflectedTopic(topic.id(), topic.title()))
				.toList();
	}

	private boolean isCompleted(CatalogEntry entry, Set<Long> completedLessonIds) {
		int totalLessons = entry.curriculum().lessons().size();
		int completedLessons = (int) entry.curriculum().lessons().stream()
				.filter(lesson -> completedLessonIds.contains(lesson.id()))
				.count();
		return CurriculumStatus.of(totalLessons, completedLessons) == CurriculumStatus.COMPLETED;
	}
}
