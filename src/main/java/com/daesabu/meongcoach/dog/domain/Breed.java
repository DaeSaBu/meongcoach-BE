package com.daesabu.meongcoach.dog.domain;

import com.daesabu.meongcoach.dog.domain.exception.InvalidBreedException;

/**
 * 강아지 견종.
 */
public enum Breed {
	POODLE("푸들"),
	MALTESE("말티즈"),
	POMERANIAN("포메라니안"),
	SHIH_TZU("시츄"),
	JINDO("진돗개"),
	GOLDEN_RETRIEVER("골든 리트리버"),
	MIXED("믹스견"),
	;

	private final String label;

	Breed(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	/**
	 * 문자열 코드를 enum으로 변환한다.
	 * 실패 시 우리 에러 코드를 유지하기 위해 도메인에서 직접 변환한다.
	 */
	public static Breed from(String value) {
		if (value == null) {
			throw new InvalidBreedException(null);
		}
		try {
			return valueOf(value);
		} catch (IllegalArgumentException e) {
			throw new InvalidBreedException(value);
		}
	}
}
