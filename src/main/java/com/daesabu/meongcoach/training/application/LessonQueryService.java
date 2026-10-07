package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.entitlement.application.provided.EntitlementChecker;
import com.daesabu.meongcoach.training.application.provided.LessonFinder;
import com.daesabu.meongcoach.training.application.required.LessonRepository;
import com.daesabu.meongcoach.training.domain.Card;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.exception.LessonNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonQueryService implements LessonFinder {
	private final LessonRepository lessonRepository;
	private final EntitlementChecker entitlementChecker;

	@Override
	public List<Card> findCards(Long userId, Long lessonId) {
		Lesson lesson = find(lessonId);

		lesson.getCurriculum().findRequiredEntitlementType()
				.ifPresent(type -> entitlementChecker.validateEntitlement(userId, type));

		return lesson.getCards();
	}

	@Override
	public Lesson find(Long lessonId) {
		return lessonRepository.findById(lessonId)
				.orElseThrow(() -> new LessonNotFoundException(lessonId));
	}
}
