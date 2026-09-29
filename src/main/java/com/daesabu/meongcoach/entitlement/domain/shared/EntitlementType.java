package com.daesabu.meongcoach.entitlement.domain.shared;

/**
 * 판매하는 이용권 종류. RevenueCat 대시보드의 이용권과는 설정(meongcoach.revenuecat.entitlement-ids)의 entitlement ID로 연결한다.
 * entitlement가 부여·확인하고 training이 콘텐츠와 연결하므로, 모듈마다 복제하지 않고 여기 한 곳에서 정의해 공개한다.
 * 상수 이름은 entitlements.type 컬럼에 저장되므로 한번 정한 뒤에는 바꾸지 않는다.
 * 대시보드에 이용권을 추가하려면 이 enum과 설정을 먼저 배포한다. 모르는 ID의 이용권은 동기화에서 무시되기 때문이다.
 */
public enum EntitlementType {
	PUPPY,
	JUNIOR,
	ADULT,
	SENIOR,
	;
}
