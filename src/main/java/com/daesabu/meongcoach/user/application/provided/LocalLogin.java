package com.daesabu.meongcoach.user.application.provided;

public interface LocalLogin {

	/**
	 * 이메일·비밀번호로 시드된 테스트 계정을 확인하고 우리 서비스 토큰을 발급한다.
	 * 가입 API가 없으므로 회원을 새로 만들지 않는다.
	 */
	LoginResult login(String email, String password);
}
