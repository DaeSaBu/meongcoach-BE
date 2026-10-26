package com.daesabu.meongcoach.user.application.provided;

import com.daesabu.meongcoach.user.application.provided.dto.UserOnboardingRequest;
import jakarta.validation.Valid;

public interface UserRegister {

	/**
	 * 온보딩 전 회원을 새로 등록하고 회원 ID를 반환한다. 자격증명 연결은 호출하는 쪽(auth 모듈)이 같은 트랜잭션에서 맡는다.
	 */
	Long register();

	/**
	 * 입력받은 프로필을 생성하고 정회원(USER)으로 승격한다. 회원이 없으면 UserNotFoundException,
	 * 이미 정회원이면 AlreadyOnboardedException을 던진다.
	 */
	void completeOnboarding(Long userId, @Valid UserOnboardingRequest request);

	void withdraw(Long userId);
}
