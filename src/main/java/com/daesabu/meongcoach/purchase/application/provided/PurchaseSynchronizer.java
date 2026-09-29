package com.daesabu.meongcoach.purchase.application.provided;

/**
 * 결제 대행에 기록된 회원의 구매를 우리 서버로 가져오는 능력.
 */
public interface PurchaseSynchronizer {

	/**
	 * 회원이 소유한 스토어 구매 중 아직 기록하지 않은 구매를 등록하고 이용권을 부여한다. 여러 번 호출해도 결과가 같다.
	 * 우리가 모르는 스토어·이용권의 구매는 건너뛰고 나머지를 등록한다.
	 * 결제 대행에서 내역을 받아 오지 못하면 {@code StorePurchaseUnavailableException}을 던진다.
	 */
	void synchronize(Long userId);
}
