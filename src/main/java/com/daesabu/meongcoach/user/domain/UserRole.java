package com.daesabu.meongcoach.user.domain;

/**
 * 회원 역할. 신규 가입자는 ONBOARDING_MEMBER로 시작해 온보딩 완료 시 MEMBER로 승격된다.
 * 게스트 로그인(U-0102)은 아직 발급 경로가 없어 어떤 URL 인가 규칙에도 매칭되지 않는다(전부 403).
 */
public enum UserRole {
	MEMBER,
	ONBOARDING_MEMBER,
	GUEST,
}
