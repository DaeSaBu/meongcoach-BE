package com.daesabu.meongcoach.user.domain;

import com.daesabu.meongcoach.shared.security.AuthorityRole;

public enum UserRole {
	USER(AuthorityRole.USER),
	ONBOARDING_USER(AuthorityRole.ONBOARDING_USER),
	ADMIN(AuthorityRole.ADMIN),
	EXPERT(AuthorityRole.EXPERT),
	TRAINER(AuthorityRole.TRAINER),
	OPERATOR(AuthorityRole.OPERATOR);

	private final AuthorityRole authorityRole;

	UserRole(AuthorityRole authorityRole) {
		this.authorityRole = authorityRole;
	}

	public AuthorityRole authorityRole() {
		return authorityRole;
	}
}
