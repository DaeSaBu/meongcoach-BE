package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.progress.application.provided.LessonProgressRecorder;
import com.daesabu.meongcoach.training.application.provided.LessonCompleter;
import com.daesabu.meongcoach.training.application.required.LessonRepository;
import com.daesabu.meongcoach.training.domain.exception.LessonNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 레슨 완료 서비스. 레슨 존재를 확인한 뒤 진행도 기록은 progress 모듈의 공개 API에 위임한다.
 */
@Service
@RequiredArgsConstructor
public class LessonCompleteService implements LessonCompleter {

	private final LessonRepository lessonRepository;

	private final LessonProgressRecorder lessonProgressRecorder;

	@Override
	@Transactional
	public int completeLesson(Long userId, Long lessonId) {
		if (!lessonRepository.existsById(lessonId)) {
			throw new LessonNotFoundException(lessonId);
		}

		return lessonProgressRecorder.completeLesson(userId, lessonId);
	}
}
