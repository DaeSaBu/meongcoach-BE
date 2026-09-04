package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.shared.security.AuthorityRole;
import com.daesabu.meongcoach.user.application.provided.RegisteredUserChecker;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.UserRole;
import com.daesabu.meongcoach.user.domain.UserStatus;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 토큰의 회원 ID가 실제로 등록된 회원인지 확인한다.
 * 인증 경로에서 요청마다 호출되므로 엔티티를 읽지 않고 필요한 값만 조회한다.
 * 탈퇴는 행을 남기는 soft delete라 존재 여부만으로는 걸러지지 않으므로, 여기서 탈퇴 회원을 미등록으로 취급해
 * 저장하지 않는 액세스 토큰도 탈퇴 즉시 401로 끝나게 한다. 리프레시 토큰은 탈퇴 시 폐기되므로 재발급 경로는 이중 방어다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegisteredUserCheckService implements RegisteredUserChecker {

	private final UserRepository userRepository;

	@Override
	public boolean isRegistered(Long userId) {
		return userRepository.existsByIdAndStatusNot(userId, UserStatus.WITHDRAWN);
	}

	@Override
	public Optional<AuthorityRole> findRole(Long userId) {
		return userRepository.findRoleByIdAndStatusNot(userId, UserStatus.WITHDRAWN)
				.map(UserRole::authorityRole);
	}
}
