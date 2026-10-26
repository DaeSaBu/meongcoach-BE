package com.daesabu.meongcoach.plan.application;

import com.daesabu.meongcoach.plan.application.provided.CurrentPlanFinder;
import com.daesabu.meongcoach.plan.application.provided.dto.CurrentPlanResult;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanCurriculumProgressResult;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanLessonProgressResult;
import com.daesabu.meongcoach.plan.application.required.PlanRepository;
import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.plan.domain.PlanStatus;
import com.daesabu.meongcoach.training.application.provided.PlanCurriculumFinder;
import com.daesabu.meongcoach.training.application.provided.dto.PlanCurriculumResult;
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
	private final PlanCurriculumFinder planCurriculumFinder;

	@Override
	public Optional<CurrentPlanResult> find(Long userId) {
		return planRepository.findByUserIdAndStatus(userId, PlanStatus.ACTIVE)
				.or(() -> planRepository.findFirstByUserIdAndStatusOrderByCompletedAtDescIdDesc(userId, PlanStatus.COMPLETED))
				.map(plan -> toResult(userId, plan));
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
		return new PlanCurriculumProgressResult(curriculum.id(), curriculum.title(), curriculum.description(),
				totalLessons, completedLessons, status, curriculum.locked(), planLessons);
	}

}
