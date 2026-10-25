package com.daesabu.meongcoach.plan.application;

import com.daesabu.meongcoach.plan.application.provided.PlanLessonCompleter;
import com.daesabu.meongcoach.plan.application.required.PlanRepository;
import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.plan.domain.PlanStatus;
import com.daesabu.meongcoach.plan.domain.exception.PlanLessonOrderException;
import com.daesabu.meongcoach.plan.domain.exception.PlanNotActiveException;
import com.daesabu.meongcoach.plan.domain.exception.PlanNotFoundException;
import com.daesabu.meongcoach.training.application.provided.LessonCompleter;
import com.daesabu.meongcoach.training.application.provided.PlanCurriculumFinder;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanLessonCompletionService implements PlanLessonCompleter {

	private final PlanRepository planRepository;
	private final PlanCurriculumFinder planCurriculumFinder;
	private final LessonCompleter lessonCompleter;

	@Override
	@Transactional
	public void complete(Long userId, Long planId, Long lessonId) {
		Plan plan = planRepository.findByIdAndUserId(planId, userId)
				.orElseThrow(PlanNotFoundException::new);

		if (plan.getStatus() == PlanStatus.REPLACED) {
			throw new PlanNotActiveException();
		}
		if (plan.getCompletedLessonIds().contains(lessonId)) {
			return;
		}
		if (plan.getStatus() != PlanStatus.ACTIVE) {
			throw new PlanNotActiveException();
		}

		List<Long> orderedLessonIds = planCurriculumFinder.findLessonIds(plan.getCurriculumIds());
		Long currentLessonId = plan.findCurrentLessonId(orderedLessonIds)
				.orElseThrow(PlanLessonOrderException::new);
		if (!currentLessonId.equals(lessonId)) {
			throw new PlanLessonOrderException();
		}

		lessonCompleter.completeLesson(userId, lessonId);
		plan.completeLesson(lessonId);

		if (plan.hasCompletedAllLessons(orderedLessonIds)) {
			plan.complete(Instant.now());
		}
	}
}
