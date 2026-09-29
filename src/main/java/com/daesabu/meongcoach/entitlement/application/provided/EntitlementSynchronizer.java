package com.daesabu.meongcoach.entitlement.application.provided;

/**
 * 결제 대행에 기록된 회원의 활성 이용권을 우리 서버의 이용권으로 옮겨 오는 능력.
 */
public interface EntitlementSynchronizer {

	/**
	 * 회원의 이용권을 결제 대행이 알려 준 활성 이용권과 같게 맞춘다. 새로 생긴 이용권은 부여하고, 빠진 이용권은 회수한다.
	 * 상태를 통째로 맞추므로 여러 번 호출해도 결과가 같다. 탈퇴한 회원이면 아무것도 하지 않는다.
	 * 결제 대행에서 내역을 받아 오지 못하면 {@code EntitlementProviderUnavailableException}을 던진다.
	 */
	void synchronize(Long userId);
}
