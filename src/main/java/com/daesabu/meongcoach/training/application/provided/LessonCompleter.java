package com.daesabu.meongcoach.training.application.provided;

public interface LessonCompleter {

	/**
	 * 강아지의 레슨 완료 횟수를 1 늘리고 갱신된 횟수를 반환한다. 레슨이 없으면 {@code LessonNotFoundException}을 던진다.
	 * 강아지 ID가 없으면 사용자가 선택한 강아지에 기록한다.
	 * 지정한 강아지가 본인 소유가 아니거나 선택된 강아지가 없으면 {@code DogNotFoundException}을 던진다.
	 * 이용권이 필요한 유료 커리큘럼의 레슨을 사용자가 이용권 없이 완료하면 진행도를 바꾸지 않고
	 * {@code EntitlementRequiredException}을 던진다. 같은 강아지·레슨의 완료가 동시에 처리되면 먼저 커밋한 완료만 반영하고
	 * 나머지는 {@code LessonCompletionConflictException}을 던진다.
	 */
	int completeLesson(Long userId, Long dogId, Long lessonId);
}
