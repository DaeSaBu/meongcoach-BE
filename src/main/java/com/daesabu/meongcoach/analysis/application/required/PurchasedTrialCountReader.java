package com.daesabu.meongcoach.analysis.application.required;

/**
 * 결제 대행에 기록된 회원의 AI 분석 구매 횟수를 읽는다.
 */
public interface PurchasedTrialCountReader {

	/**
	 * 회원이 구매해 보유한 AI 분석 횟수를 반환한다. 환불된 구매는 빠진 값이다. 결제 대행에 회원이 없거나 구매 내역이 없으면 0을 반환한다. 결제 대행에서 내역을 받아 오지 못하면
	 * {@code AiTrialProviderUnavailableException}을 던진다.
	 */
	int readPurchasedCount(Long userId);
}
