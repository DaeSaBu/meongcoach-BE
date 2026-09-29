package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.user.application.provided.UserFinder;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserQueryService implements UserFinder {
	private final UserRepository userRepository;

	// 없는 회원은 false가 아니라 예외다. 탈퇴 회원과 구분해야 없는 회원의 구매 등록을 실패로 드러낼 수 있다
	@Override
	public boolean isActiveUser(Long userId) {
		User user = findById(userId);
		return user.isActive();
	}

	// 온보딩 상태의 단일 원천은 users.role이다. 다른 모듈은 User 엔티티를 볼 수 없으므로 판정 결과만 내준다
	@Override
	public boolean isOnboardingUser(Long userId) {
		User user = findById(userId);
		return user.isOnboarding();
	}

	@Override
	public User findById(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
	}
}
