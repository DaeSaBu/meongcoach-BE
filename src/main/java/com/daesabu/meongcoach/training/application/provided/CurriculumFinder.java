package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.application.provided.dto.CurriculumDetailResult;
import com.daesabu.meongcoach.training.application.provided.dto.CurriculumListResult;

/**
 * 커리큘럼 조회 능력.
 */
public interface CurriculumFinder {

	/**
	 * 사용자가 마지막으로 진입한 토픽(없으면 첫 토픽)의 커리큘럼과 완료 레슨 수·진행 상태·잠금 여부를 조회한다.
	 */
	CurriculumListResult findCurriculums(Long userId);

	/**
	 * 커리큘럼과 소속 레슨별 사용자의 반복 완료 횟수를 조회한다. 기록이 없는 레슨은 0이다.
	 * 이용권이 필요한 유료 커리큘럼을 사용자가 이용권 없이 조회하면 {@code EntitlementRequiredException}을 던진다.
	 */
	CurriculumDetailResult findCurriculum(Long userId, Long curriculumId);
}
