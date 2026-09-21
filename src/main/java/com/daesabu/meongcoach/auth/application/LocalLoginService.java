package com.daesabu.meongcoach.auth.application;

import com.daesabu.meongcoach.auth.application.provided.AuthToken;
import com.daesabu.meongcoach.auth.application.provided.LocalLogin;
import com.daesabu.meongcoach.auth.application.provided.LoginResult;
import com.daesabu.meongcoach.auth.application.required.LocalAccountRepository;
import com.daesabu.meongcoach.auth.domain.Email;
import com.daesabu.meongcoach.auth.domain.LocalAccount;
import com.daesabu.meongcoach.auth.domain.PasswordMatcher;
import com.daesabu.meongcoach.auth.domain.exception.InvalidCredentialsException;
import com.daesabu.meongcoach.auth.domain.exception.WithdrawnUserException;
import com.daesabu.meongcoach.shared.security.AuthorityRole;
import com.daesabu.meongcoach.user.application.provided.RegisteredUserChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 스토어 심사용 테스트 계정의 이메일·비밀번호 로그인. 계정은 시드로만 만들어지므로 조회·대조·발급만 한다.
 * 이메일 미존재와 비밀번호 불일치는 같은 예외로 응답해 계정 존재 여부를 드러내지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocalLoginService implements LocalLogin {

	private final LocalAccountRepository localAccountRepository;
	private final AuthTokenIssueService authTokenIssueService;
	private final PasswordMatcher passwordMatcher;
	private final RegisteredUserChecker registeredUserChecker;

	// 토큰 발급이 리프레시 토큰 행을 저장하므로 클래스 기본값(readOnly)을 쓰기 트랜잭션으로 덮어쓴다
	@Override
	@Transactional
	public LoginResult login(Email email, String password) {
		LocalAccount account = findAccount(email);
		validatePassword(password, account);

		Long userId = account.getUserId();
		// 비밀번호 대조 뒤에 확인해야 탈퇴 여부가 비밀번호를 모르는 쪽에 드러나지 않는다.
		// 등록된 회원만 role이 조회된다. 자격증명이 남아 있는데 조회되지 않으면 탈퇴한 회원이다
		AuthorityRole role = registeredUserChecker.findRole(userId)
				.orElseThrow(WithdrawnUserException::new);

		AuthToken token = authTokenIssueService.issue(userId);

		boolean needsOnboarding = role == AuthorityRole.ONBOARDING_USER;

		return new LoginResult(token, needsOnboarding);
	}

	private LocalAccount findAccount(Email email) {
		return localAccountRepository.findByEmail(email)
				.orElseThrow(InvalidCredentialsException::new);
	}

	private void validatePassword(String password, LocalAccount account) {
		if (!account.isValidPassword(password, passwordMatcher)) {
			throw new InvalidCredentialsException();
		}
	}
}
