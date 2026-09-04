package com.daesabu.meongcoach.user.domain;

import com.daesabu.meongcoach.user.domain.exception.InvalidMbtiException;

/**
 * 사용자 MBTI 16가지 유형.
 */
public enum Mbti {
	ISTJ, ISFJ, INFJ, INTJ,
	ISTP, ISFP, INFP, INTP,
	ESTP, ESFP, ENFP, ENTP,
	ESTJ, ESFJ, ENFJ, ENTJ,
	;

	/**
	 * 문자열 코드를 enum으로 변환한다. 실패 시 우리 에러 코드를 유지하기 위해 도메인에서 직접 변환한다.
	 */
	public static Mbti from(String value) {
		if (value == null) {
			throw new InvalidMbtiException(null);
		}
		try {
			return valueOf(value);
		} catch (IllegalArgumentException e) {
			throw new InvalidMbtiException(value);
		}
	}
}
