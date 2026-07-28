package com.daesabu.meongcoach.user.domain;

import com.daesabu.meongcoach.user.domain.exception.InvalidGenderException;

/**
 * 사용자 성별. 선택 입력이며 NONE은 '응답하지 않음'을 뜻한다 — 미입력(null)과 구분된다.
 */
public enum Gender {
	MALE,
	FEMALE,
	NONE,
	;

	/**
	 * 문자열 코드를 enum으로 변환한다. 실패 시 우리 에러 코드를 유지하기 위해 도메인에서 직접 변환한다.
	 */
	public static Gender from(String value) {
		if (value == null) {
			throw new InvalidGenderException(null);
		}
		try {
			return valueOf(value);
		} catch (IllegalArgumentException e) {
			throw new InvalidGenderException(value);
		}
	}
}
