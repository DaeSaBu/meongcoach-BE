package com.daesabu.meongcoach.auth.application;

import com.daesabu.meongcoach.auth.application.provided.AccountFinder;
import com.daesabu.meongcoach.auth.application.provided.AccountRegister;
import com.daesabu.meongcoach.auth.application.provided.AuthTokenProvider;
import com.daesabu.meongcoach.auth.application.provided.Authenticator;
import com.daesabu.meongcoach.auth.application.provided.RefreshTokenFinder;
import com.daesabu.meongcoach.auth.application.provided.RefreshTokenRegister;
import com.daesabu.meongcoach.auth.application.provided.dto.EmailAccountFindRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.EmailLoginRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.LogoutRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.RefreshTokenFindRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.RefreshTokenRevokeRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.SocialAccountRegisterRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.SocialLoginRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.TokenRefreshRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.WithdrawRequest;
import com.daesabu.meongcoach.auth.application.required.TokenProvider;
import com.daesabu.meongcoach.auth.domain.AuthToken;
import com.daesabu.meongcoach.auth.domain.Email;
import com.daesabu.meongcoach.auth.domain.EmailAccount;
import com.daesabu.meongcoach.auth.domain.PasswordMatcher;
import com.daesabu.meongcoach.auth.domain.RefreshToken;
import com.daesabu.meongcoach.auth.domain.RefreshTokenId;
import com.daesabu.meongcoach.auth.domain.SocialAccount;
import com.daesabu.meongcoach.auth.domain.SocialProfile;
import com.daesabu.meongcoach.auth.domain.SocialProvider;
import com.daesabu.meongcoach.auth.domain.exception.InvalidCredentialsException;
import com.daesabu.meongcoach.auth.domain.exception.InvalidRefreshTokenException;
import com.daesabu.meongcoach.auth.domain.exception.WithdrawnUserException;
import com.daesabu.meongcoach.user.application.provided.UserFinder;
import com.daesabu.meongcoach.user.application.provided.UserRegister;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class AuthenticationService implements Authenticator {
	private final AccountFinder accountFinder;
	private final AccountRegister accountRegister;
	private final PasswordMatcher passwordMatcher;
	private final AuthTokenProvider authTokenProvider;
	private final TokenProvider tokenProvider;
	private final RefreshTokenRegister refreshTokenRegister;
	private final SocialProfileReaders socialProfileReaders;
	private final SocialTokenRevokers socialTokenRevokers;
	private final RefreshTokenFinder refreshTokenFinder;
	private final UserFinder userFinder;
	private final UserRegister userRegister;

	// 제공자 호출은 롤백할 것이 없고 그동안 커넥션을 잡을 이유도 없으므로 클래스 기본 트랜잭션을 끈다.
	// 계정 등록(AccountRegister)과 토큰 발급·저장(AuthTokenProvider)은 각자 트랜잭션을 연다
	@Override
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public AuthToken socialLogin(SocialLoginRequest socialLoginRequest) {
		SocialProfile socialProfile = readSocialProfile(socialLoginRequest);

		Long userId = upsertSocialAccount(socialProfile);
		validateNotWithdrawn(userId);

		return authTokenProvider.issue(userId);
	}

	@Override
	@Transactional
	public AuthToken emailLogin(EmailLoginRequest emailLoginRequest) {
		Email email = new Email(emailLoginRequest.email());
		EmailAccount emailAccount = getEmailAccount(email);

		validatePassword(emailAccount, emailLoginRequest.password());

		// 비밀번호 대조 뒤에 확인해야 탈퇴 여부가 비밀번호를 모르는 쪽에 드러나지 않는다
		Long userId = emailAccount.getUserId();
		validateNotWithdrawn(userId);

		return authTokenProvider.issue(userId);
	}

	@Override
	@Transactional
	public void logout(LogoutRequest logoutRequest) {
		refreshTokenRegister.revoke(new RefreshTokenRevokeRequest(logoutRequest.refreshToken()));
	}

	@Override
	@Transactional
	public AuthToken refresh(TokenRefreshRequest tokenRefreshRequest) {
		RefreshTokenId tokenId = tokenProvider.extractTokenId(tokenRefreshRequest.refreshToken());
		RefreshToken refreshToken = refreshTokenFinder.findByTokenId(new RefreshTokenFindRequest(tokenId));
		validateRefreshTokenExpireAt(refreshToken);

		Long userId = refreshToken.getUserId();
		validateActiveUser(userId);

		refreshToken.revoke();

		return authTokenProvider.issue(userId);
	}

	@Override
	@Transactional
	public void withdraw(Long userId, WithdrawRequest withdrawRequest) {
		List<SocialAccount> socialAccounts = accountFinder.findAllSocialAccount(userId);

		// 소셜 계정을 지우기 전에 revoke해야 실패했을 때 자격증명이 남아 새 코드로 다시 시도할 수 있다
		String authorizationCode = authorizationCode(withdrawRequest);
		socialAccounts.forEach(account ->
				socialTokenRevokers.revokeIfSupported(account.getProvider(), authorizationCode));

		accountRegister.deleteAllSocialAccounts(userId);
		accountRegister.deleteAllEmailAccounts(userId);
		refreshTokenRegister.revokeAllByUserId(userId);
		userRegister.withdraw(userId);
	}

	// Apple 계정 회원만 본문을 보내므로 나머지 회원의 탈퇴 요청에는 본문이 없다
	private String authorizationCode(WithdrawRequest withdrawRequest) {
		if (withdrawRequest == null) {
			return null;
		}
		return withdrawRequest.appleAuthorizationCode();
	}

	// 탈퇴는 자격증명을 지우지만, 자격증명이 남은 탈퇴 회원에게도 토큰을 내주지 않는다
	private void validateNotWithdrawn(Long userId) {
		if (userFinder.isActiveUser(userId)) {
			return;
		}
		throw new WithdrawnUserException();
	}

	private void validateActiveUser(Long userId) {
		if (userFinder.isActiveUser(userId)) {
			return;
		}
		throw new InvalidRefreshTokenException();
	}

	private void validateRefreshTokenExpireAt(RefreshToken refreshToken) {
		if (refreshToken.isUsable(LocalDateTime.now())) {
			return;
		}

		throw new InvalidRefreshTokenException();
	}

	private EmailAccount getEmailAccount(Email email) {
		return accountFinder.findEmailAccount(new EmailAccountFindRequest(email));
	}

	private void validatePassword(EmailAccount account, String password) {
		if (account.isValidPassword(password, passwordMatcher)) {
			return;
		}

		throw new InvalidCredentialsException();
	}

	private SocialProfile readSocialProfile(SocialLoginRequest socialLoginRequest) {
		SocialProvider provider = SocialProvider.from(socialLoginRequest.socialProvider());

		return socialProfileReaders.read(provider, socialLoginRequest.idToken());
	}

	private Long upsertSocialAccount(SocialProfile socialProfile) {
		return accountRegister.upsertSocialAccount(new SocialAccountRegisterRequest(
						socialProfile.provider(),
						socialProfile.providerId(),
						socialProfile.email()
				)
		);
	}
}
