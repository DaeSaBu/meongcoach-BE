package com.daesabu.meongcoach.training.application.provided;

import java.util.List;

public interface CurriculumCompletionChecker {

	/**
	 * 회원이 지정한 커리큘럼의 모든 레슨을 완료했는지 확인한다.
	 * 빈 목록이면 false를 반환한다.
	 * 존재하지 않는 커리큘럼이 있으면 CurriculumNotFoundException을 던진다.
	 * 레슨이 없는 커리큘럼이 있으면 IllegalStateException을 던진다.
	 */
	boolean areAllCompleted(Long userId, List<Long> curriculumIds);
}
