package com.daesabu.meongcoach.user.application.provided;

import com.daesabu.meongcoach.user.domain.command.UserProfileCreateCommand;

/**
 * 사용자 프로필 등록 공개 API. 온보딩 완료 여부는 프로필 행이 아니라 회원 role로 판단한다.
 */
public interface UserProfileRegister {

	/**
	 * 온보딩 완료 시점에 사용자 프로필을 생성한다. 이미 정회원(USER)이면 예외를 던진다.
	 */
	void register(Long userId, UserProfileCreateCommand command);
}
