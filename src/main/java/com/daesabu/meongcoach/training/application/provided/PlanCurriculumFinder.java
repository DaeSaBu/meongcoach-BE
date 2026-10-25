package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.application.provided.dto.PlanCurriculumResult;
import java.util.List;

public interface PlanCurriculumFinder {

	/**
	 * 요청한 ID 순서로 커리큘럼 설명과 잠금 여부를 반환한다.
	 * 각 커리큘럼의 레슨은 현재 교육 순서로 반환한다.
	 * 존재하지 않는 커리큘럼은 CurriculumNotFoundException을 던진다.
	 * 레슨이 없는 커리큘럼은 IllegalStateException을 던진다.
	 */
	List<PlanCurriculumResult> find(Long userId, List<Long> curriculumIds);
}
