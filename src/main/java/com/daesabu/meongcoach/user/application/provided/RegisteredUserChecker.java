package com.daesabu.meongcoach.user.application.provided;

import com.daesabu.meongcoach.user.domain.UserRole;
import java.util.Optional;

public interface RegisteredUserChecker {

	/**
	 * 해당 ID의 회원이 등록되어 있는지 확인한다. 상태(탈퇴 등)는 보지 않는다. (토큰 재발급 경로용)
	 */
	boolean isRegistered(Long userId);

	/**
	 * 등록된 회원의 역할을 조회한다. 미등록이면 empty라 존재 확인을 겸한다. (액세스 토큰 인증 경로용)
	 * 시그니처의 UserRole은 user 모듈 내부 소비자만 있어 허용된다 — 타 모듈에 열 때는 provided 패키지 record로 변환할 것.
	 */
	Optional<UserRole> findRole(Long userId);
}
