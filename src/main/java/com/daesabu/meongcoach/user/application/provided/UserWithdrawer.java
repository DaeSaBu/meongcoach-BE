package com.daesabu.meongcoach.user.application.provided;

public interface UserWithdrawer {

	/**
	 * 회원을 탈퇴 처리한다. 자격증명(소셜·로컬 계정)과 프로필은 삭제하고 회원 행은 WITHDRAWN 상태로 남긴다.
	 * 자격증명이 사라지므로 같은 소셜 계정으로 다시 로그인하면 새 회원으로 가입된다.
	 * 제공자 연결 해제(revoke)가 필요한 소셜 계정은 revoke가 먼저 성공해야 탈퇴되며, 실패하면 아무것도 바꾸지 않고 예외를 던진다.
	 *
	 * @param socialAuthorizationCode 탈퇴 직전 소셜 제공자 재인증으로 받은 1회용 인가 코드.
	 *                                필수 여부는 제공자별 revoker가 정하며(현재 Apple만 필수), revoke 대상이 아닌
	 *                                제공자 회원은 null이고 값이 있어도 무시한다
	 * @throws com.daesabu.meongcoach.user.domain.exception.AppleAuthorizationCodeRequiredException Apple 계정 회원이 코드 없이 요청한 경우
	 * @throws com.daesabu.meongcoach.user.domain.exception.InvalidAppleAuthorizationCodeException Apple이 코드를 거부한 경우
	 * @throws com.daesabu.meongcoach.user.domain.exception.SocialProviderUnavailableException Apple과 통신할 수 없는 경우
	 */
	void withdraw(Long userId, String socialAuthorizationCode);
}
