package com.daesabu.meongcoach.user.application.provided;

public interface UserRegister {

	/**
	 * 온보딩 전 회원을 새로 등록하고 회원 ID를 반환한다. 자격증명 연결은 호출하는 쪽(auth 모듈)이 같은 트랜잭션에서 맡는다.
	 */
	Long register();
}
