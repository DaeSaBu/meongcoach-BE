package com.daesabu.meongcoach.ai.application.provided;

import com.daesabu.meongcoach.ai.domain.AiTrial;

/**
 * AI 리포트 무료 체험 횟수 조회 능력.
 */
public interface AiTrialFinder {

	/**
	 * 사용자의 무료 체험 사용 현황을 조회한다. 저장된 현황이 없으면 기본 부여 횟수에 사용 횟수 0인 현황을 저장하지 않고 반환한다.
	 */
	AiTrial findTrial(Long userId);
}
