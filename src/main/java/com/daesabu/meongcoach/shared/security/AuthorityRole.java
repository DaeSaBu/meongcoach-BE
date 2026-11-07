package com.daesabu.meongcoach.shared.security;

public enum AuthorityRole {
	USER,
	ONBOARDING_USER,
	ADMIN,
	EXPERT,
	TRAINER;

	private static final String AUTHORITY_PREFIX = "ROLE_";

	public String authority() {
		return AUTHORITY_PREFIX + name();
	}
}
