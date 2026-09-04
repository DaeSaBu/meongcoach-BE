package com.daesabu.meongcoach.user.application.provided;

import com.daesabu.meongcoach.shared.security.AuthorityRole;
import java.util.Optional;

public interface RegisteredUserChecker {

	/**
	 * 해당 ID의 회원이 등록되어 있는지 확인한다. 탈퇴한 회원은 미등록으로 취급한다. (토큰 재발급 경로용)
	 */
	boolean isRegistered(Long userId);

	/**
	 * 등록된 회원의 인가 어휘를 조회한다. 미등록·탈퇴면 empty라 존재 확인을 겸한다. (액세스 토큰 인증 경로용)
	 * 도메인 상태(UserRole)가 아닌 shared 어휘로 반환하므로 모듈 경계를 넘어도 안전하다.
	 */
	Optional<AuthorityRole> findRole(Long userId);
}
