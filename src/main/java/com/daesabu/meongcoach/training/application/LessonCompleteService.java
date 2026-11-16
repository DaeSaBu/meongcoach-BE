package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.entitlement.application.provided.EntitlementChecker;
import com.daesabu.meongcoach.training.application.provided.LessonCompleter;
import com.daesabu.meongcoach.training.application.provided.LessonFinder;
import com.daesabu.meongcoach.training.application.required.LessonProgressRepository;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonProgress;
import com.daesabu.meongcoach.training.domain.TrainingSource;
import com.daesabu.meongcoach.training.domain.exception.LessonCompletionConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonCompleteService implements LessonCompleter {
	private final LessonFinder lessonFinder;
	private final EntitlementChecker entitlementChecker;
	private final LessonProgressRepository lessonProgressRepository;
	private final DogProfileFinder dogProfileFinder;
	private final TrainedLessonRecordService trainedLessonRecordService;

	@Override
	@Transactional
	public int completeLesson(Long userId, Long dogId, Long lessonId) {
		return complete(userId, dogId, lessonId, TrainingSource.LIBRARY);
	}

	private int complete(Long userId, Long dogId, Long lessonId, TrainingSource source) {
		Lesson lesson = lessonFinder.find(lessonId);
		lesson.findRequiredEntitlementType().ifPresent(type -> entitlementChecker.validateEntitlement(userId, type));
		Long resolvedDogId = dogProfileFinder.findDogProfileOrSelected(userId, dogId).id();
		LessonProgress progress = lessonProgressRepository
				.findByUserIdAndDogIdAndLessonId(userId, resolvedDogId, lessonId)
				.orElseGet(() -> LessonProgress.start(userId, resolvedDogId, lessonId));
		int completedCount = progress.complete();
		try {
			lessonProgressRepository.saveAndFlush(progress);
		} catch (DataIntegrityViolationException | OptimisticLockingFailureException e) {
			throw new LessonCompletionConflictException(lessonId, e);
		}
		trainedLessonRecordService.record(userId, resolvedDogId, lesson, source);
		return completedCount;
	}
}
