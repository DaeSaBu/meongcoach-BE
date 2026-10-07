package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.application.provided.dto.CurriculumDetailResult;
import com.daesabu.meongcoach.training.application.provided.dto.CurriculumListResult;

/**
 * 커리큘럼 조회 능력.
 */
public interface CurriculumFinder {

	/**
	 * 사용자가 마지막으로 진입한 토픽(없으면 첫 토픽)의 커리큘럼과, 그 레슨 중 사용자가 완료한 레슨 id를 조회한다.
	 * 토픽 카테고리의 이용권을 사용자가 지금 쓸 수 있는지 함께 반환하며, 이용권이 필요 없는 카테고리면 true다.
	 */
	CurriculumListResult findCurriculums(Long userId);

	/**
	 * 커리큘럼과 소속 레슨별 사용자의 반복 완료 횟수를 조회한다. 기록이 없는 레슨은 0이다.
	 * 이용권이 필요한 유료 커리큘럼을 사용자가 이용권 없이 조회하면 {@code EntitlementRequiredException}을 던진다.
	 */
	CurriculumDetailResult findCurriculum(Long userId, Long curriculumId);
}
