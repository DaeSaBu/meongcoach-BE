package com.daesabu.meongcoach.onboarding.application.provided;

import java.util.List;

/**
 * 온보딩 완료 공개 API.
 */
public interface OnboardingCompleter {

	/**
	 * 사용자 프로필과 강아지들을 하나의 트랜잭션으로 생성하고, 생성된 강아지 ID 목록을 반환한다.
	 */
	List<Long> complete(Long userId, OnboardingCompleteInfo info);
}
