package com.daesabu.meongcoach.shared.security;

/**
 * 인가에 쓰는 회원 역할 어휘의 단일 원천. TokenType과 같은 부류(영속되지 않는 보안 어휘)로,
 * user 모듈의 UserRole(영속 도메인 상태)이 이 어휘로 자신을 투영하고
 * SecurityConfig의 URL 인가 규칙과 GlobalExceptionHandler의 403 분기가 같은 어휘를 소비한다.
 * shared가 user를 참조할 수 없으므로 어휘의 원천을 shared에 두고 user가 참조하는 방향으로 역전했다.
 */
public enum AuthorityRole {
	MEMBER,
	ONBOARDING_MEMBER,
	GUEST;

	private static final String AUTHORITY_PREFIX = "ROLE_";

	/**
	 * 스프링 시큐리티 GrantedAuthority 문자열(ROLE_ 접두어 포함).
	 */
	public String authority() {
		return AUTHORITY_PREFIX + name();
	}
}
