package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.provided.LocalLogin;
import com.daesabu.meongcoach.user.application.provided.LoginResult;
import com.daesabu.meongcoach.user.application.required.LocalAccountRepository;
import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.domain.LocalAccount;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserStatus;
import com.daesabu.meongcoach.user.domain.exception.InvalidCredentialsException;
import com.daesabu.meongcoach.user.domain.exception.WithdrawnUserException;
import com.daesabu.meongcoach.user.domain.vo.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
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
	private final UserProfileRepository userProfileRepository;
	private final AuthTokenIssueService authTokenIssueService;
	private final PasswordEncoder passwordEncoder;

	// 토큰 발급이 리프레시 토큰 행을 저장하므로 클래스 기본값(readOnly)을 쓰기 트랜잭션으로 덮어쓴다
	@Override
	@Transactional
	public LoginResult login(String email, String password) {
		LocalAccount account = localAccountRepository.findByEmail(new Email(email))
				.orElseThrow(InvalidCredentialsException::new);
		if (!passwordEncoder.matches(password, account.getPasswordHash())) {
			throw new InvalidCredentialsException();
		}
		// 비밀번호 대조 뒤에 확인해야 탈퇴 여부가 비밀번호를 모르는 쪽에 드러나지 않는다
		User user = account.getUser();
		if (user.getStatus() == UserStatus.WITHDRAWN) {
			throw new WithdrawnUserException();
		}

		AuthToken token = authTokenIssueService.issue(user);
		boolean needsOnboarding = !userProfileRepository.existsById(user.getId());
		return new LoginResult(token, needsOnboarding);
	}
}
