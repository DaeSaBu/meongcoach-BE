package com.daesabu.meongcoach.dog.domain;

import com.daesabu.meongcoach.dog.domain.exception.InvalidPersonalityException;

/**
 * 강아지 성격 유형.
 */
public enum Personality {
	TIMID("소심함"),
	LIVELY("활발함"),
	FRIENDLY("친화적"),
	;

	private final String label;

	Personality(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	/**
	 * 문자열 코드를 enum으로 변환한다. 실패 시 우리 에러 코드를 유지하기 위해 도메인에서 직접 변환한다.
	 */
	public static Personality from(String value) {
		if (value == null) {
			throw new InvalidPersonalityException(null);
		}
		try {
			return valueOf(value);
		} catch (IllegalArgumentException e) {
			throw new InvalidPersonalityException(value);
		}
	}
}
