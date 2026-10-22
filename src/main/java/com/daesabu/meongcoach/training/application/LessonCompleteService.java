package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.entitlement.application.provided.EntitlementChecker;
import com.daesabu.meongcoach.training.application.provided.LessonCompleter;
import com.daesabu.meongcoach.training.application.provided.LessonFinder;
import com.daesabu.meongcoach.training.application.required.LessonProgressRepository;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonProgress;
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

	@Override
	@Transactional
	public int completeLesson(Long userId, Long lessonId) {
		Lesson lesson = lessonFinder.find(lessonId);
		lesson.findRequiredEntitlementType().ifPresent(type -> entitlementChecker.validateEntitlement(userId, type));
		LessonProgress progress = lessonProgressRepository.findByUserIdAndLessonId(userId, lessonId)
				.orElseGet(() -> LessonProgress.start(userId, lessonId));
		int completedCount = progress.complete();
		try {
			lessonProgressRepository.saveAndFlush(progress);
		} catch (DataIntegrityViolationException | OptimisticLockingFailureException e) {
			throw new LessonCompletionConflictException(lessonId, e);
		}
		return completedCount;
	}
}
