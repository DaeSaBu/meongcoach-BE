package com.daesabu.meongcoach.user.domain;

/**
 * 회원 상태. 탈퇴(U-0705)는 soft delete로 처리한다.
 */
public enum UserStatus {
	ACTIVE,
	DORMANT,
	WITHDRAWN,
}
