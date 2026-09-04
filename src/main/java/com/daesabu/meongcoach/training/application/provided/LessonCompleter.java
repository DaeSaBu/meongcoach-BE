package com.daesabu.meongcoach.training.application.provided;

/**
 * 레슨 완료 능력. 사용자가 레슨을 끝냈을 때 반복 완료 횟수를 올린다.
 */
public interface LessonCompleter {

	/**
	 * 레슨 완료를 기록하고, 증가가 반영된 반복 완료 횟수를 반환한다.
	 *
	 * @throws com.daesabu.meongcoach.training.domain.exception.LessonNotFoundException 레슨이 없으면 발생한다
	 */
	int completeLesson(Long userId, Long lessonId);
}
