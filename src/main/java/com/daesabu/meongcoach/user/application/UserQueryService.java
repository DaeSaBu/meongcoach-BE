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

	@Override
	public boolean isActiveUser(Long userId) {
		User user = findById(userId);
		return user.isActive();
	}

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
