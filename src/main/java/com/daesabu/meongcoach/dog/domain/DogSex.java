package com.daesabu.meongcoach.dog.domain;

import com.daesabu.meongcoach.dog.domain.exception.InvalidDogSexException;

/**
 * 강아지 성별. ERD에 값이 명시되지 않아 가정한 값이다 — 기획 확인 필요(중성화 여부 포함 가능성).
 */
public enum DogSex {
	MALE,
	FEMALE,
	;

	/**
	 * 문자열 코드를 enum으로 변환한다. 실패 시 우리 에러 코드를 유지하기 위해 도메인에서 직접 변환한다.
	 */
	public static DogSex from(String value) {
		if (value == null) {
			throw new InvalidDogSexException(null);
		}
		try {
			return valueOf(value);
		} catch (IllegalArgumentException e) {
			throw new InvalidDogSexException(value);
		}
	}
}
