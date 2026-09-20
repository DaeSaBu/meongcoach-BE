package com.daesabu.meongcoach.user.domain;

import com.daesabu.meongcoach.shared.security.AuthorityRole;

/**
 * 회원 역할. 신규 가입자는 ONBOARDING_MEMBER로 시작해 온보딩 완료 시 MEMBER로 승격된다.
 * 인가 어휘(AuthorityRole)와의 매핑을 선언부에 두어 문자열 원천을 shared 한 곳으로 유지한다.
 */
public enum UserRole {
	MEMBER(AuthorityRole.MEMBER),
	ONBOARDING_MEMBER(AuthorityRole.ONBOARDING_MEMBER);

	private final AuthorityRole authorityRole;

	UserRole(AuthorityRole authorityRole) {
		this.authorityRole = authorityRole;
	}

	public AuthorityRole authorityRole() {
		return authorityRole;
	}
}
