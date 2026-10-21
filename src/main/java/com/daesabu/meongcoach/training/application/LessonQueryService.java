package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.entitlement.application.provided.EntitlementChecker;
import com.daesabu.meongcoach.training.application.provided.LessonFinder;
import com.daesabu.meongcoach.training.application.provided.LessonProgressFinder;
import com.daesabu.meongcoach.training.application.provided.dto.LessonSummaryResult;
import com.daesabu.meongcoach.training.application.required.LessonProgressRepository;
import com.daesabu.meongcoach.training.application.required.LessonRepository;
import com.daesabu.meongcoach.training.domain.Card;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonProgress;
import com.daesabu.meongcoach.training.domain.LessonProgresses;
import com.daesabu.meongcoach.training.domain.exception.LessonNotFoundException;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonQueryService implements LessonFinder, LessonProgressFinder {
	private final LessonProgressRepository lessonProgressRepository;
	private final LessonRepository lessonRepository;
	private final EntitlementChecker entitlementChecker;

	@Override
	public List<Card> findCards(Long userId, Long lessonId) {
		Lesson lesson = find(lessonId);

		lesson.findRequiredEntitlementType()
				.ifPresent(type -> entitlementChecker.validateEntitlement(userId, type));

		return lesson.getCards();
	}

	@Override
	public Lesson find(Long lessonId) {
		return lessonRepository.findById(lessonId)
				.orElseThrow(() -> new LessonNotFoundException(lessonId));
	}

	@Override
	public List<LessonSummaryResult> findSummaries(Collection<Long> lessonIds) {
		if (lessonIds.isEmpty()) {
			return List.of();
		}
		return lessonRepository.findAllById(lessonIds).stream()
				.map(LessonSummaryResult::from)
				.toList();
	}
	@Override
	public LessonProgresses findProgresses(Long userId, Collection<Long> lessonIds) {
		List<LessonProgress> progresses = lessonProgressRepository.findAllByUserIdAndLessonIdIn(userId, lessonIds);
		return LessonProgresses.from(progresses);
	}

}
