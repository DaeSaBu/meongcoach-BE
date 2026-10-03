package com.daesabu.meongcoach.training.application.provided;

/**
 * 커리큘럼 조회 능력.
 */
public interface CurriculumFinder {

	/**
	 * 사용자가 마지막으로 진입한 토픽(없으면 첫 토픽)의 커리큘럼과, 그 레슨 중 사용자가 완료한 레슨 id를 조회한다.
	 */
	CurriculumListResult findCurriculums(Long userId);

	/**
	 * 커리큘럼과 소속 레슨별 사용자의 반복 완료 횟수를 조회한다. 기록이 없는 레슨은 0이다.
	 */
	CurriculumDetailResult findCurriculum(Long userId, Long curriculumId);
}
