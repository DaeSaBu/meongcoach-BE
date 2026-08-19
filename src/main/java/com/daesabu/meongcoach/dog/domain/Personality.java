package com.daesabu.meongcoach.dog.domain;

import com.daesabu.meongcoach.dog.domain.exception.InvalidPersonalityException;

/**
 * 강아지 성격 특성. 보호자가 관찰한 특성을 중복 선택하는 평면 목록이며,
 * 서로 반대되는 항목(활발함/차분함 등)의 동시 선택은 막지 않는다.
 */
public enum Personality {
	TIMID("소심함"),
	LIVELY("활발함"),
	FRIENDLY("친화적"),
	CALM("차분함"),
	FEARFUL("겁 많음"),
	AFFECTIONATE("애교 많음"),
	INDEPENDENT("독립적"),
	PLAYFUL("장난기 많음"),
	EXCITABLE("쉽게 흥분함"),
	STUBBORN("고집 셈"),
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
