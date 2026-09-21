package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.user.application.provided.UserFinder;
import com.daesabu.meongcoach.user.application.provided.UserRegister;
import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserRegisterService implements UserRegister {
	private final UserFinder userFinder;
	private final UserRepository userRepository;
	private final UserProfileRepository userProfileRepository;

	@Override
	@Transactional
	public Long register() {
		User user = userRepository.save(User.registerUser());

		return user.getId();
	}

	@Override
	@Transactional
	public void withdraw(Long userId) {
		User user = userFinder.findById(userId);

		// 개인정보인 프로필 행은 실제로 지운다. 온보딩 미완료 회원은 프로필이 없으므로 없으면 무시하는 deleteById를 쓴다
		userProfileRepository.deleteById(userId);
		user.withdraw();

		userRepository.save(user);
	}
}
