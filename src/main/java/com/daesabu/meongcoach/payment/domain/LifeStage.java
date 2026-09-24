package com.daesabu.meongcoach.payment.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 이용권이 열어 주는 강아지 성장 시기. RevenueCat 대시보드의 entitlement 하나에 대응한다.
 * 상수 이름은 entitlements.life_stage 컬럼에 그대로 저장되는 값이라 한번 정한 뒤에는 바꾸지 않는다.
 */
@Getter
@RequiredArgsConstructor
public enum LifeStage {
	PUPPY("puppy"),
	JUNIOR("junior"),
	ADULT("adult"),
	SENIOR("senior"),
	;

	// RevenueCat entitlement 식별자(웹훅 entitlement_ids의 값)
	private final String identifier;
}
