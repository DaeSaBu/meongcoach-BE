package com.daesabu.meongcoach.progress.application.provided;

/**
 * 레슨 완료 기록 능력. 다른 모듈은 이 인터페이스로만 진행도를 기록한다.
 */
public interface LessonProgressRecorder {

	/**
	 * 레슨 완료를 기록하고, 증가한 뒤의 반복 완료 횟수를 반환한다.
	 */
	int completeLesson(Long userId, Long lessonId);
}
