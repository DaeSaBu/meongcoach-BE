package com.daesabu.meongcoach.user.application.provided;

/**
 * 이메일·비밀번호 로컬 계정 생성 공개 API. 앱에는 가입 화면이 없고, 부하 테스트 모듈이 테스트 계정을 만들 때만 쓴다.
 */
public interface LocalAccountRegister {

	/**
	 * 온보딩 전 상태의 회원과 로컬 계정을 함께 만들고 생성된 회원 ID를 반환한다.
	 * 이미 등록된 이메일이면 {@code DuplicateEmailException}을 던지고 생성하지 않는다.
	 */
	Long register(LocalAccountRegisterInfo info);
}
