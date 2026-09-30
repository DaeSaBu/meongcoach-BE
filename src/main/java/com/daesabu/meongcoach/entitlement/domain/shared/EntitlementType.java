package com.daesabu.meongcoach.entitlement.domain.shared;

/**
 * 판매하는 이용권 종류. 모두 기간 없이 계속 쓰는 평생 이용권이다.
 * entitlement가 부여·확인하고 training이 콘텐츠와 연결하므로, 모듈마다 복제하지 않고 여기 한 곳에서 정의해 공개한다.
 * 상수 이름은 entitlements.type 컬럼에 저장되므로 한번 정한 뒤에는 바꾸지 않는다.
 * RevenueCat 대시보드의 이용권과 연결하는 일은 이 타입이 아니라 RevenueCat 연동 어댑터가 맡는다.
 */
public enum EntitlementType {
	PUPPY,
	JUNIOR,
	ADULT,
	SENIOR,
	;
}
