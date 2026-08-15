package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.user.application.provided.RegisteredUserChecker;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.UserRole;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 토큰의 회원 ID가 실제로 등록된 회원인지 확인한다.
 * 인증 경로에서 요청마다 호출되므로 엔티티를 읽지 않고 필요한 값만 조회한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegisteredUserCheckService implements RegisteredUserChecker {

	private final UserRepository userRepository;

	@Override
	public boolean isRegistered(Long userId) {
		return userRepository.existsById(userId);
	}

	@Override
	public Optional<UserRole> findRole(Long userId) {
		return userRepository.findRoleById(userId);
	}
}
