package com.daesabu.meongcoach.onboarding.application.provided;

import com.daesabu.meongcoach.onboarding.application.provided.dto.OnboardingCompleteRequest;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 온보딩 완료 공개 API.
 */
public interface OnboardingCompleter {

	/**
	 * 기본 보호자 프로필과 강아지를 하나의 트랜잭션으로 생성하고, 생성된 강아지 ID 목록을 반환한다.
	 */
	List<Long> complete(Long userId, @Valid OnboardingCompleteRequest request);
}
