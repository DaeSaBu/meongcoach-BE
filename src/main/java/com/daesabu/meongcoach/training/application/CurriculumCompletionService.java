package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.training.application.provided.CurriculumCompletionChecker;
import com.daesabu.meongcoach.training.application.required.CurriculumRepository;
import com.daesabu.meongcoach.training.application.required.LessonProgressRepository;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.LessonProgress;
import com.daesabu.meongcoach.training.domain.LessonProgresses;
import com.daesabu.meongcoach.training.domain.exception.CurriculumNotFoundException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurriculumCompletionService implements CurriculumCompletionChecker {

	private final CurriculumRepository curriculumRepository;
	private final LessonProgressRepository lessonProgressRepository;

	@Override
	public boolean areAllCompleted(Long userId, List<Long> curriculumIds) {
		if (curriculumIds.isEmpty()) {
			return false;
		}

		List<Curriculum> curriculums = curriculumRepository.findAllById(curriculumIds);
		validateCurriculumsExist(curriculumIds, curriculums);

		List<Long> lessonIds = curriculums.stream()
				.flatMap(curriculum -> curriculum.getLessonIds().stream())
				.toList();

		List<LessonProgress> lessonProgresses =
				lessonProgressRepository.findAllByUserIdAndLessonIdIn(userId, lessonIds);
		LessonProgresses progresses = LessonProgresses.from(lessonProgresses);

		boolean allCompleted = true;
		for (Curriculum curriculum : curriculums) {
			if (!curriculum.isCompleted(progresses)) {
				allCompleted = false;
			}
		}
		return allCompleted;
	}

	private void validateCurriculumsExist(List<Long> curriculumIds, List<Curriculum> curriculums) {
		Set<Long> existingCurriculumIds = curriculums.stream()
				.map(Curriculum::getId)
				.collect(Collectors.toSet());

		for (Long curriculumId : curriculumIds) {
			if (!existingCurriculumIds.contains(curriculumId)) {
				throw new CurriculumNotFoundException(curriculumId);
			}
		}
	}
}
