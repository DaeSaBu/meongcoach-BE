package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.application.provided.dto.PlanCurriculumResult;
import java.util.List;

public interface PlanCurriculumFinder {

	/**
	 * 요청한 ID 순서로 커리큘럼 설명, 레슨 진도와 잠금 여부를 반환한다.
	 * 존재하지 않는 커리큘럼은 CurriculumNotFoundException을 던진다.
	 */
	List<PlanCurriculumResult> find(Long userId, List<Long> curriculumIds);
}
