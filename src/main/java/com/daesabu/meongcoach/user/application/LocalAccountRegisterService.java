package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.user.application.provided.LocalAccountRegister;
import com.daesabu.meongcoach.user.application.provided.LocalAccountRegisterInfo;
import com.daesabu.meongcoach.user.application.required.LocalAccountRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.LocalAccount;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.command.LocalAccountCreateCommand;
import com.daesabu.meongcoach.user.domain.exception.DuplicateEmailException;
import com.daesabu.meongcoach.user.domain.vo.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이메일·비밀번호 로컬 계정 생성. 회원(User)과 자격증명(LocalAccount)은 한 트랜잭션에서 함께 만든다.
 * 앱 회원가입이 아니라 부하 테스트 계정 생성 용도라 이메일 인증 없이 바로 등록한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocalAccountRegisterService implements LocalAccountRegister {

	private final UserRepository userRepository;
	private final LocalAccountRepository localAccountRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public Long register(LocalAccountRegisterInfo info) {
		Email email = new Email(info.email());
		if (localAccountRepository.existsByEmail(email)) {
			throw new DuplicateEmailException();
		}

		User user = userRepository.save(User.registerOnboardingMember());
		String passwordHash = passwordEncoder.encode(info.password());
		localAccountRepository.save(LocalAccount.create(user, new LocalAccountCreateCommand(email, passwordHash)));
		return user.getId();
	}
}
