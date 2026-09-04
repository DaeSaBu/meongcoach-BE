package com.daesabu.meongcoach.user.application.provided;

import java.util.List;

/**
 * 사람 MBTI 목록 조회 공개 API.
 */
public interface MbtiFinder {

	/**
	 * 모든 MBTI 코드를 반환한다. 코드가 곧 표시 문자열이다.
	 */
	List<String> findAllCodes();
}
