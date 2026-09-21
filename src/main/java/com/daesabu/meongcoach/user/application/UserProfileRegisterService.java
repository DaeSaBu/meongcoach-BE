package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.user.application.provided.UserProfileRegister;
import com.daesabu.meongcoach.user.application.provided.UserProfileRegisterRequest;
import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfile;
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
	public void register(Long userId, UserProfileRegisterRequest request) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(userId));
		// 이미 USER면 여기서 AlreadyOnboardedException. 인가가 요청마다 DB의 role을 읽으므로 승격은 토큰 재발급 없이 즉시 반영된다
		user.promoteToUser();

		userProfileRepository.save(UserProfile.create(user, request.toCommand()));
	}
}
