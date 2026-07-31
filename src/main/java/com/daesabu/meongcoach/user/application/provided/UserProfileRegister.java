package com.daesabu.meongcoach.user.application.provided;

/**
 * 사용자 프로필 등록 공개 API. 프로필 행 존재 여부가 곧 온보딩 완료 여부다.
 */
public interface UserProfileRegister {

	/**
	 * 온보딩 완료 시점에 사용자 프로필을 생성한다. 이미 프로필이 있으면 예외를 던진다.
	 */
	void register(Long userId, UserProfileCreateInfo info);
}
