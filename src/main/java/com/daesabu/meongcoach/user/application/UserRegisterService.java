package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.media.application.provided.StoredMediaVerifier;
import com.daesabu.meongcoach.user.application.provided.UserFinder;
import com.daesabu.meongcoach.user.application.provided.UserProfileRegisterRequest;
import com.daesabu.meongcoach.user.application.provided.UserRegister;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfileImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserRegisterService implements UserRegister {
	private final UserFinder userFinder;
	private final UserRepository userRepository;
	private final StoredMediaVerifier storedMediaVerifier;

	@Override
	@Transactional
	public Long register() {
		User user = userRepository.save(User.registerUser());

		return user.getId();
	}

	@Override
	@Transactional
	public void completeOnboarding(Long userId, UserProfileRegisterRequest request) {
		storedMediaVerifier.validateUrl(UserProfileImage.SCOPE, userId, request.profileImageUrl());
		User user = userFinder.findById(userId);

		user.completeOnboarding(request.toCommand());

		userRepository.save(user);
	}

	@Override
	@Transactional
	public void withdraw(Long userId) {
		User user = userFinder.findById(userId);

		user.withdraw();

		userRepository.save(user);
	}
}
