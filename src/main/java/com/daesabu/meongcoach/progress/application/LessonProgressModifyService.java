package com.daesabu.meongcoach.progress.application;

import com.daesabu.meongcoach.progress.application.provided.LessonProgressUpdater;
import com.daesabu.meongcoach.progress.application.required.LessonProgressRepository;
import com.daesabu.meongcoach.progress.domain.LessonProgress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonProgressModifyService implements LessonProgressUpdater {
	private final LessonProgressRepository lessonProgressRepository;

	@Override
	@Transactional
	public int updateCompletion(Long userId, Long lessonId) {
		LessonProgress progress = lessonProgressRepository.findByUserIdAndLessonId(userId, lessonId)
				.orElseGet(() -> LessonProgress.start(userId, lessonId));

		progress.increaseCompletedCount();
		lessonProgressRepository.save(progress);
		return progress.getCompletedCount();
	}
}
