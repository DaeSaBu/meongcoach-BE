package com.daesabu.meongcoach.user.domain;

/**
 * 회원 유형. 게스트 로그인(U-0102)은 기록 저장·동기화 범위가 제한된다.
 */
public enum UserType {
	MEMBER,
	GUEST,
}
