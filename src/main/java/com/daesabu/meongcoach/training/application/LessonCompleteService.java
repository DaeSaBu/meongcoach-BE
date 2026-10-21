package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.entitlement.application.provided.EntitlementChecker;
import com.daesabu.meongcoach.progress.application.provided.LessonProgressUpdater;
import com.daesabu.meongcoach.training.application.provided.LessonCompleter;
import com.daesabu.meongcoach.training.application.provided.LessonFinder;
import com.daesabu.meongcoach.training.application.provided.TopicCompletedEvent;
import com.daesabu.meongcoach.training.application.provided.TopicCompletionChecker;
import com.daesabu.meongcoach.training.domain.Lesson;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonCompleteService implements LessonCompleter {
	private final LessonProgressUpdater lessonProgressUpdater;
	private final EntitlementChecker entitlementChecker;
	private final LessonFinder lessonFinder;
	private final TopicCompletionChecker topicCompletionChecker;
	private final ApplicationEventPublisher eventPublisher;

	@Override
	@Transactional
	public int completeLesson(Long userId, Long lessonId) {
		Lesson lesson = lessonFinder.find(lessonId);

		lesson.findRequiredEntitlementType()
				.ifPresent(type -> entitlementChecker.validateEntitlement(userId, type));

		int completedCount = lessonProgressUpdater.updateCompletion(userId, lessonId);
		Long topicId = lesson.getCurriculum().getTopic().getId();
		if (topicCompletionChecker.isCompleted(userId, topicId)) {
			eventPublisher.publishEvent(new TopicCompletedEvent(userId, topicId));
		}
		return completedCount;
	}
}
