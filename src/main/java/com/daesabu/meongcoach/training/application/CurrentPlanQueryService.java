package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.training.application.provided.CurrentPlanFinder;
import com.daesabu.meongcoach.training.application.provided.PlanCurriculumFinder;
import com.daesabu.meongcoach.training.application.provided.dto.CurrentPlanResult;
import com.daesabu.meongcoach.training.application.provided.dto.PlanCurriculumProgressResult;
import com.daesabu.meongcoach.training.application.provided.dto.PlanCurriculumResult;
import com.daesabu.meongcoach.training.application.provided.dto.PlanLessonProgressResult;
import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;
import com.daesabu.meongcoach.training.application.required.PlanRepository;
import com.daesabu.meongcoach.training.domain.Plan;
import com.daesabu.meongcoach.training.domain.PlanStatus;
import com.daesabu.meongcoach.training.domain.shared.ProgressStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurrentPlanQueryService implements CurrentPlanFinder {

	private final PlanRepository planRepository;
	private final DogProfileFinder dogProfileFinder;
	private final PlanCurriculumFinder planCurriculumFinder;

	@Override
	public Optional<CurrentPlanResult> find(Long userId, Long dogId) {
		dogProfileFinder.findDogProfile(userId, dogId);

		return planRepository.findByUserIdAndDogIdAndStatus(userId, dogId, PlanStatus.ACTIVE)
				.or(() -> planRepository.findFirstByUserIdAndDogIdAndStatusOrderByCompletedAtDescIdDesc(userId, dogId, PlanStatus.COMPLETED))
				.map(plan -> toResult(userId, plan));
	}

	@Override
	public List<Long> findActiveTopicIds(Long userId, Long dogId) {
		dogProfileFinder.findDogProfile(userId, dogId);
		List<Long> curriculumIds = planRepository.findByUserIdAndDogIdAndStatus(userId, dogId, PlanStatus.ACTIVE)
				.map(Plan::getCurriculumIds)
				.orElse(List.of());
		return planCurriculumFinder.findTopics(curriculumIds).stream()
				.map(TopicResult::id)
				.toList();
	}

	private CurrentPlanResult toResult(Long userId, Plan plan) {
		if (plan.getStatus() == PlanStatus.COMPLETED) {
			List<PlanCurriculumProgressResult> curriculums = List.of();
			return new CurrentPlanResult(plan, curriculums);
		}
		List<PlanCurriculumResult> curriculums = planCurriculumFinder.find(userId, plan.getCurriculumIds());
		Set<Long> completedLessonIds = plan.getCompletedLessonIds();
		List<PlanCurriculumProgressResult> curriculumProgresses = curriculums.stream()
				.map(curriculum -> toProgressResult(curriculum, completedLessonIds))
				.toList();
		return new CurrentPlanResult(plan, curriculumProgresses);
	}

	private PlanCurriculumProgressResult toProgressResult(PlanCurriculumResult curriculum,
			Set<Long> completedLessonIds) {
		List<PlanLessonProgressResult> planLessons = curriculum.lessons().stream()
				.map(lesson -> new PlanLessonProgressResult(lesson.id(), lesson.title(),
						completedLessonIds.contains(lesson.id())))
				.toList();
		int totalLessons = planLessons.size();
		int completedLessons = (int) planLessons.stream()
				.filter(PlanLessonProgressResult::completed)
				.count();
		ProgressStatus status = ProgressStatus.of(totalLessons, completedLessons);
		return new PlanCurriculumProgressResult(curriculum.id(), curriculum.topicId(), curriculum.topicTitle(),
				curriculum.title(), curriculum.description(),
				totalLessons, completedLessons, status, curriculum.locked(), planLessons);
	}

}
