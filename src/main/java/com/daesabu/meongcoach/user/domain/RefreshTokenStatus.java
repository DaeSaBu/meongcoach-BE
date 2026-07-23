package com.daesabu.meongcoach.user.domain;

/**
 * 리프레시 토큰 상태. ERD에 값이 명시되지 않아 가정한 값이다 — 기획 확인 필요.
 */
public enum RefreshTokenStatus {
	ACTIVE,
	REVOKED,
	EXPIRED,
}
