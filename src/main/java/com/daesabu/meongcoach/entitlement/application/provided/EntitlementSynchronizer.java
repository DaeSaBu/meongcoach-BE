package com.daesabu.meongcoach.entitlement.application.provided;

/**
 * 이용권 동기화 공개 API.
 */
public interface EntitlementSynchronizer {

	/**
	 * 회원의 이용권을 결제 대행에 기록된 활성 이용권에 맞춘다.
	 * 활성 이용권의 만료 시각을 반영하고, 활성 이용권에서 빠진 이용권은 동기화 시각으로 만료시키며, 없던 종류만 새로 부여한다.
	 * 결제 대행에서 내역을 받아 오지 못하면 이용권을 바꾸지 않고 {@code EntitlementProviderUnavailableException}을 던진다.
	 */
	void synchronize(Long userId);
}
