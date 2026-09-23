package com.daesabu.meongcoach.auth.adapter.security;

import com.daesabu.meongcoach.auth.domain.PasswordMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 도메인 PasswordMatcher의 BCrypt 구현. 스프링 PasswordEncoder 빈(shared/config/SecurityConfig)에 위임한다.
 */
@Component
@RequiredArgsConstructor
public class BcryptPasswordMatcher implements PasswordMatcher {

	private final PasswordEncoder passwordEncoder;

	@Override
	public boolean matches(String rawPassword, String passwordHash) {
		return passwordEncoder.matches(rawPassword, passwordHash);
	}
}
