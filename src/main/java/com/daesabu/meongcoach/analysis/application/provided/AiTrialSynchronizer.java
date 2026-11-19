package com.daesabu.meongcoach.analysis.application.provided;

/**
 * AI 분석 구매 횟수 동기화 능력.
 */
public interface AiTrialSynchronizer {

	/**
	 * 사용자의 구매 횟수를 결제 대행에 기록된 구매 횟수로 바꾼다. 저장된 현황이 없으면 기본 부여 횟수에 구매 횟수를 더한 현황을 저장한다. 구매 횟수가 줄어 사용 횟수가 최대 횟수를 넘으면 사용 횟수를
	 * 최대 횟수로 낮춘다. 결제 대행에서 구매 횟수를 받아 오지 못하면 현황을 바꾸지 않고 {@code AiTrialProviderUnavailableException}을 던진다.
	 */
	void synchronize(Long userId);
}
