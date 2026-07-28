package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.user.application.provided.UserProfileCreateInfo;
import com.daesabu.meongcoach.user.application.provided.UserProfileRegister;
import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.Mbti;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfile;
import com.daesabu.meongcoach.user.domain.exception.AlreadyOnboardedException;
import com.daesabu.meongcoach.user.domain.exception.InvalidMbtiException;
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
	public void register(Long userId, UserProfileCreateInfo info) {
		validateUserProfileExisting(userId);

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(userId));

		UserProfile profile = UserProfile.create(user, info.nickname());
		profile.changeBirthDate(info.birthDate());
		if (info.mbti() != null) {
			profile.changeMbti(parseMbti(info.mbti()));
		}
		userProfileRepository.save(profile);
	}

	private Mbti parseMbti(String mbti) {
		try {
			return Mbti.valueOf(mbti);
		} catch (IllegalArgumentException e) {
			throw new InvalidMbtiException(mbti);
		}
	}

	private void validateUserProfileExisting(Long userId) {
		if (userProfileRepository.existsById(userId)) {
			throw new AlreadyOnboardedException();
		}
	}
}
