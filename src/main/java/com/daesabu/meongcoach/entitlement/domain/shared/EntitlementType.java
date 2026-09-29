package com.daesabu.meongcoach.entitlement.domain.shared;

import com.daesabu.meongcoach.entitlement.domain.exception.UnsupportedEntitlementTypeException;
import java.util.Arrays;
import java.util.Locale;

/**
 * 판매하는 이용권 종류. 상수 이름을 소문자로 바꾼 값이 RevenueCat 대시보드의 entitlement 식별자다.
 * entitlement가 부여·확인하고 training이 콘텐츠와 연결하므로, 모듈마다 복제하지 않고 여기 한 곳에서 정의해 공개한다.
 * 상수 이름은 entitlements.type 컬럼에 저장되므로 한번 정한 뒤에는 바꾸지 않는다.
 * 대시보드에 이용권을 추가하려면 이 enum을 먼저 배포한다. 모르는 식별자가 오면 구매 등록이 실패하기 때문이다.
 */
public enum EntitlementType {
	PUPPY,
	JUNIOR,
	ADULT,
	SENIOR,
	;

	public static EntitlementType from(String value) {
		if (!isSupported(value)) {
			throw new UnsupportedEntitlementTypeException(value);
		}
		return valueOf(value.toUpperCase(Locale.ROOT));
	}

	// 모르는 식별자를 예외 없이 가려내야 하는 호출자(구매 동기화의 건별 건너뛰기)를 위해 from과 같은 규칙으로 판정한다
	public static boolean isSupported(String value) {
		if (value == null || value.isBlank()) {
			return false;
		}
		String name = value.toUpperCase(Locale.ROOT);
		return Arrays.stream(values())
				.anyMatch(type -> type.name().equals(name));
	}
}
