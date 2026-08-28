package com.daesabu.meongcoach.user.application.provided;

public interface UserWithdrawer {

	/**
	 * 회원을 탈퇴 처리한다. 자격증명(소셜·로컬 계정)과 프로필은 삭제하고 회원 행은 WITHDRAWN 상태로 남긴다.
	 * 자격증명이 사라지므로 같은 소셜 계정으로 다시 로그인하면 새 회원으로 가입된다.
	 */
	void withdraw(Long userId);
}
