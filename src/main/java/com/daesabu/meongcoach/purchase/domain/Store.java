package com.daesabu.meongcoach.purchase.domain;

import com.daesabu.meongcoach.purchase.domain.exception.UnsupportedStoreException;
import java.util.Arrays;

/**
 * 구매가 일어난 스토어. 상수 이름이 RevenueCat store 값(소문자 app_store)을 대문자로 바꾼 것과 같고, purchases.store 컬럼에 그대로 저장된다.
 * 앱을 파는 스토어와 RevenueCat이 직접 지급·시험하는 경로만 둔다. 모르는 값이 오면 구매 등록이 실패하므로 판매처를 늘리려면 이 enum을 먼저 배포한다.
 */
public enum Store {
	APP_STORE,
	PLAY_STORE,
	PROMOTIONAL,
	TEST_STORE,
	;

	public static Store from(String value) {
		return Arrays.stream(values())
				.filter(store -> store.name().equals(value))
				.findFirst()
				.orElseThrow(() -> new UnsupportedStoreException(value));
	}
}
