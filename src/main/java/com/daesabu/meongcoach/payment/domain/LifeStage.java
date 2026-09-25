package com.daesabu.meongcoach.payment.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 이용권이 열어 주는 강아지 성장 시기. 우리 커리큘럼이 해석하는 개념이며, RevenueCat entitlement 식별자와 연결한다.
 * 권한 확인은 entitlements.identifier에 저장된 식별자를 이 매핑으로 시기에 대응시킨다.
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
