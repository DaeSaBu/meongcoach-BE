package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.user.application.provided.UserFinder;
import com.daesabu.meongcoach.user.application.provided.UserProfileRegister;
import com.daesabu.meongcoach.user.application.provided.UserProfileRegisterRequest;
import com.daesabu.meongcoach.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileRegisterService implements UserProfileRegister {

	private final UserFinder userFinder;

	@Override
	@Transactional
	public void register(Long userId, UserProfileRegisterRequest request) {
		User user = userFinder.findById(userId);

		user.completeOnboarding(request.toCommand());
	}
}
