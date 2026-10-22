package com.daesabu.meongcoach.training.application.provided;

public interface LessonCompleter {

	/**
	 * 사용자의 레슨 완료 횟수를 1 늘리고 갱신된 횟수를 반환한다. 레슨이 없으면 {@code LessonNotFoundException}을 던진다.
	 * 이용권이 필요한 유료 커리큘럼의 레슨을 사용자가 이용권 없이 완료하면 진행도를 바꾸지 않고
	 * {@code EntitlementRequiredException}을 던진다. 같은 사용자·레슨의 완료가 동시에 처리되면 먼저 커밋한 완료만 반영하고
	 * 나머지는 {@code LessonCompletionConflictException}을 던진다.
	 */
	int completeLesson(Long userId, Long lessonId);
}
