package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.application.provided.dto.CurriculumDetailResult;
import com.daesabu.meongcoach.training.application.provided.dto.CurriculumListResult;

/**
 * 커리큘럼 조회 능력.
 */
public interface CurriculumFinder {

	/**
	 * 강아지가 마지막으로 진입한 토픽(없으면 첫 토픽)의 커리큘럼과 완료 레슨 수·진행 상태·잠금 여부를 조회한다.
	 * 강아지 ID가 없으면 사용자가 선택한 강아지의 기록을 쓴다.
	 * 지정한 강아지가 본인 소유가 아니거나 선택된 강아지가 없으면 {@code DogNotFoundException}을 던진다.
	 */
	CurriculumListResult findCurriculums(Long userId, Long dogId);

	/**
	 * 커리큘럼과 소속 레슨별 강아지의 반복 완료 횟수를 조회한다. 기록이 없는 레슨은 0이다.
	 * 이용권이 필요한 유료 커리큘럼을 사용자가 이용권 없이 조회하면 {@code EntitlementRequiredException}을 던진다.
	 * 강아지 ID가 없으면 사용자가 선택한 강아지의 기록을 쓴다.
	 * 지정한 강아지가 본인 소유가 아니거나 선택된 강아지가 없으면 {@code DogNotFoundException}을 던진다.
	 */
	CurriculumDetailResult findCurriculum(Long userId, Long dogId, Long curriculumId);
}
