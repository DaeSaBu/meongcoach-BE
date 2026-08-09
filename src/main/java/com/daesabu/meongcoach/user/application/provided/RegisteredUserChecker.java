package com.daesabu.meongcoach.user.application.provided;

public interface RegisteredUserChecker {

	/**
	 * 해당 ID의 회원이 등록되어 있는지 확인한다. 상태(탈퇴 등)는 보지 않는다.
	 */
	boolean isRegistered(Long userId);
}
