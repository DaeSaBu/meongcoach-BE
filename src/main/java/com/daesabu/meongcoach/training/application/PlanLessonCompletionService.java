package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.training.application.provided.PlanCurriculumFinder;
import com.daesabu.meongcoach.training.application.provided.PlanLessonCompleter;
import com.daesabu.meongcoach.training.application.required.LessonRepository;
import com.daesabu.meongcoach.training.application.required.PlanRepository;
import com.daesabu.meongcoach.training.domain.Plan;
import com.daesabu.meongcoach.training.domain.TrainingSource;
import com.daesabu.meongcoach.training.domain.exception.PlanNotFoundException;
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
	private final LessonCompleteService lessonCompleteService;
	private final LessonRepository lessonRepository;
	private final TrainedLessonRecordService trainedLessonRecordService;

	@Override
	@Transactional
	public void complete(Long userId, Long planId, Long lessonId) {
		Plan plan = planRepository.findByIdAndUserId(planId, userId)
				.orElseThrow(PlanNotFoundException::new);

		if (!plan.needsLessonCompletion(lessonId)) {
			lessonRepository.findById(lessonId)
					.ifPresent(lesson -> trainedLessonRecordService.record(userId, plan.getDogId(), lesson,
							TrainingSource.PLAN));
			return;
		}

		List<List<Long>> topicLessonIds = planCurriculumFinder.findTopicLessonIds(plan.getCurriculumIds());
		plan.verifyCurrentLesson(lessonId, topicLessonIds);
		lessonCompleteService.completePlanLesson(userId, plan.getDogId(), lessonId);
		plan.completeCurrentLesson(lessonId, topicLessonIds, Instant.now());
	}
}
