package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.entitlement.application.provided.EntitlementChecker;
import com.daesabu.meongcoach.progress.application.provided.LessonProgressUpdater;
import com.daesabu.meongcoach.training.application.provided.LessonCompleter;
import com.daesabu.meongcoach.training.application.required.LessonRepository;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.exception.LessonNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonCompleteService implements LessonCompleter {

	private final LessonRepository lessonRepository;

	private final LessonProgressUpdater lessonProgressUpdater;

	private final EntitlementChecker entitlementChecker;

	@Override
	@Transactional
	public int completeLesson(Long userId, Long lessonId) {
		Lesson lesson = lessonRepository.findById(lessonId)
				.orElseThrow(() -> new LessonNotFoundException(lessonId));
		lesson.getCurriculum().findRequiredEntitlementType()
				.ifPresent(type -> entitlementChecker.validateEntitlement(userId, type));

		return lessonProgressUpdater.updateCompletion(userId, lessonId);
	}
}
