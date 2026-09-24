package com.daesabu.meongcoach.payment.domain;

import java.util.EnumSet;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 판매 상품. RevenueCat 대시보드의 product와 그 상품에 연결된 entitlement 구성을 그대로 옮긴 것이라, 대시보드에서 구성을 바꾸면 함께 바꾼다.
 * 상수 이름은 entitlements.product_id 컬럼에 그대로 저장되는 값이라 한번 정한 뒤에는 바꾸지 않는다.
 */
@Getter
@RequiredArgsConstructor
public enum ProductId {
	PUPPY_LIFETIME("meongcoach_puppy_lifetime", EnumSet.of(LifeStage.PUPPY)),
	JUNIOR_LIFETIME("meongcoach_junior_lifetime", EnumSet.of(LifeStage.JUNIOR)),
	ADULT_LIFETIME("meongcoach_adult_lifetime", EnumSet.of(LifeStage.ADULT)),
	SENIOR_LIFETIME("meongcoach_senior_lifetime", EnumSet.of(LifeStage.SENIOR)),
	ALL_LIFETIME("meongcoach_all_lifetime", EnumSet.allOf(LifeStage.class)),
	;

	// RevenueCat 상품 식별자(웹훅 product_id의 값)
	private final String identifier;
	private final Set<LifeStage> lifeStages;
}
