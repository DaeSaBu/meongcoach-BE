package com.daesabu.meongcoach.entitlement.application.provided;

import java.time.Instant;

/**
 * 이용권 지급 공개 API. 학습 보상처럼 결제 없이 이용권을 주는 경우에 쓴다.
 */
public interface EntitlementGranter {

	/**
	 * 회원에게 모든 종류의 이용권을 같은 만료 시각으로 결제 대행에 지급한 뒤, 이용권 동기화로 서버 DB에 반영한다.
	 * 한 종류라도 실패하면 {@code EntitlementProviderUnavailableException}을 던지고 남은 종류는 지급하지 않는다.
	 */
	void grant(Long userId, Instant expiresAt);
}
