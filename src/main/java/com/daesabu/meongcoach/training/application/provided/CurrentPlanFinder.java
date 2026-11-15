package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.application.provided.dto.CurrentPlanResult;
import java.util.List;
import java.util.Optional;

public interface CurrentPlanFinder {

	/**
	 * 본인 소유 강아지의 활성 플랜을 우선 반환하고 없으면 해당 강아지의 최근 완료 플랜을 반환한다.
	 * 강아지가 없거나 본인 소유가 아니면 DogNotFoundException을 던진다.
	 */
	Optional<CurrentPlanResult> find(Long userId, Long dogId);

	/**
	 * 본인 소유 강아지의 활성 플랜에 든 커리큘럼의 토픽 ID를 플랜 순서대로 중복 없이 반환한다.
	 * 활성 플랜이 없으면 빈 목록을 반환한다.
	 * 강아지가 없거나 본인 소유가 아니면 DogNotFoundException을 던진다.
	 */
	List<Long> findActiveTopicIds(Long userId, Long dogId);
}
