package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.user.application.provided.UserRegister;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserRegisterService implements UserRegister {

	private final UserRepository userRepository;

	@Override
	@Transactional
	public Long register() {
		User user = userRepository.save(User.registerUser());
		return user.getId();
	}
}
