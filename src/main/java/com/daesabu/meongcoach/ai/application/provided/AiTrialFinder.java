package com.daesabu.meongcoach.ai.application.provided;

import com.daesabu.meongcoach.ai.domain.vo.AiTrial;

/**
 * AI 리포트 무료 체험 횟수 조회 능력.
 */
public interface AiTrialFinder {

	/**
	 * 사용자의 무료 체험 사용 현황을 조회한다. 완료된 리포트만 세며, 없으면 사용 횟수 0으로 내려간다.
	 */
	AiTrial findTrial(Long userId);
}
