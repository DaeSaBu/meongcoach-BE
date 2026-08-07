package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.user.application.provided.UserProfileRegister;
import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfile;
import com.daesabu.meongcoach.user.domain.command.UserProfileCreateCommand;
import com.daesabu.meongcoach.user.domain.exception.AlreadyOnboardedException;
import com.daesabu.meongcoach.user.domain.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 온보딩 완료 시점의 사용자 프로필 생성을 담당한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileRegisterService implements UserProfileRegister {

	private final UserRepository userRepository;
	private final UserProfileRepository userProfileRepository;

	@Override
	@Transactional
	public void register(Long userId, UserProfileCreateCommand command) {
		validateUserProfileExisting(userId);

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(userId));

		userProfileRepository.save(UserProfile.create(user, command));
	}

	private void validateUserProfileExisting(Long userId) {
		if (userProfileRepository.existsById(userId)) {
			throw new AlreadyOnboardedException();
		}
	}
}
