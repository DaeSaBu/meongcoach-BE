package com.daesabu.meongcoach.training.application.provided;

/**
 * 커리큘럼 조회 능력.
 */
public interface CurriculumFinder {

	/**
	 * 사용자가 마지막으로 진입한 토픽의 커리큘럼을 진행 상태와 함께 조회한다.
	 */
	CurriculumListView findCurriculums(Long userId);

	/**
	 * 커리큘럼 하나를 소속 레슨·사용자의 반복 완료 횟수와 함께 조회한다.
	 */
	CurriculumDetailView findCurriculum(Long userId, Long curriculumId);
}
