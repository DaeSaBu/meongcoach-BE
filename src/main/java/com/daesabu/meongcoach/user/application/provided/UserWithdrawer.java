package com.daesabu.meongcoach.user.application.provided;

public interface UserWithdrawer {

	/**
	 * 회원을 탈퇴 처리한다. 프로필은 삭제하고 회원 행은 WITHDRAWN 상태로 남긴다.
	 * 자격증명·토큰 정리는 auth 모듈이 이 호출에 앞서 같은 트랜잭션에서 수행한다.
	 */
	void withdraw(Long userId);
}
