package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.media.application.provided.StoredMediaVerifier;
import com.daesabu.meongcoach.user.application.provided.UserFinder;
import com.daesabu.meongcoach.user.application.provided.UserRegister;
import com.daesabu.meongcoach.user.application.provided.dto.UserOnboardingRequest;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfileCreateCommand;
import com.daesabu.meongcoach.user.domain.UserProfileImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class UserRegisterService implements UserRegister {
	private final UserFinder userFinder;
	private final StoredMediaVerifier storedMediaVerifier;
	private final UserRepository userRepository;

	@Override
	@Transactional
	public Long register() {
		User user = userRepository.save(User.registerUser());

		return user.getId();
	}

	@Override
	@Transactional
	public void completeOnboarding(Long userId, UserOnboardingRequest request) {
		User user = userFinder.findById(userId);

		storedMediaVerifier.validateUrl(UserProfileImage.SCOPE, userId, request.profileImageUrl());
		UserProfileCreateCommand command = request.toCommand();
		user.completeOnboarding(command);

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
