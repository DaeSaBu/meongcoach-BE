package com.daesabu.meongcoach.payment.domain;

/**
 * 구매가 일어난 스토어. 상수 이름이 RevenueCat 웹훅 store 값과 같다.
 * TEST_STORE는 RevenueCat Test Store 샌드박스 구매다.
 * 상수 이름은 entitlements.store 컬럼에 그대로 저장되는 값이라 한번 정한 뒤에는 바꾸지 않는다.
 */
public enum Store {
	APP_STORE,
	PLAY_STORE,
	TEST_STORE,
}
