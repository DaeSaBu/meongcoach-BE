package com.daesabu.meongcoach.user.domain;

import com.daesabu.meongcoach.shared.security.AuthorityRole;

/**
 * 회원 역할. 신규 가입자는 ONBOARDING_MEMBER로 시작해 온보딩 완료 시 MEMBER로 승격된다.
 * 게스트 로그인(U-0102)은 아직 발급 경로가 없어 어떤 URL 인가 규칙에도 매칭되지 않는다(전부 403).
 * 인가 어휘(AuthorityRole)와의 매핑을 선언부에 두어 문자열 원천을 shared 한 곳으로 유지한다.
 */
public enum UserRole {
	MEMBER(AuthorityRole.MEMBER),
	ONBOARDING_MEMBER(AuthorityRole.ONBOARDING_MEMBER),
	GUEST(AuthorityRole.GUEST);

	private final AuthorityRole authorityRole;

	UserRole(AuthorityRole authorityRole) {
		this.authorityRole = authorityRole;
	}

	public AuthorityRole authorityRole() {
		return authorityRole;
	}
}
