package com.daesabu.meongcoach.analysis.application.provided;

/**
 * AI 리포트 무료 체험 횟수 변경 능력.
 */
public interface AiTrialRegister {

	/**
	 * 사용자의 부여 횟수를 {@code count}만큼 늘린다. 저장된 현황이 없으면 기본 부여 횟수에 {@code count}를 더한 현황을 저장한다.
	 */
	void grant(Long userId, int count);
}
