package com.daesabu.meongcoach.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.auth.application.provided.dto.EmailLoginRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.LogoutRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.SocialLoginRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.TokenRefreshRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.WithdrawRequest;
import com.daesabu.meongcoach.auth.application.required.EmailAccountRepository;
import com.daesabu.meongcoach.auth.application.required.RefreshTokenRepository;
import com.daesabu.meongcoach.auth.application.required.SocialAccountRepository;
import com.daesabu.meongcoach.auth.application.required.SocialProfileReader;
import com.daesabu.meongcoach.auth.application.required.SocialTokenRevoker;
import com.daesabu.meongcoach.auth.application.required.TokenProvider;
import com.daesabu.meongcoach.auth.domain.AuthToken;
import com.daesabu.meongcoach.auth.domain.Email;
import com.daesabu.meongcoach.auth.domain.EmailAccountFixture;
import com.daesabu.meongcoach.auth.domain.RefreshToken;
import com.daesabu.meongcoach.auth.domain.RefreshTokenId;
import com.daesabu.meongcoach.auth.domain.RefreshTokenRegisterCommand;
import com.daesabu.meongcoach.auth.domain.SocialAccount;
import com.daesabu.meongcoach.auth.domain.SocialAccountRegisterCommand;
import com.daesabu.meongcoach.auth.domain.SocialProfile;
import com.daesabu.meongcoach.auth.domain.SocialProvider;
import com.daesabu.meongcoach.auth.domain.exception.InvalidAppleAuthorizationCodeException;
import com.daesabu.meongcoach.auth.domain.exception.InvalidCredentialsException;
import com.daesabu.meongcoach.auth.domain.exception.InvalidEmailException;
import com.daesabu.meongcoach.auth.domain.exception.InvalidRefreshTokenException;
import com.daesabu.meongcoach.auth.domain.exception.UnsupportedSocialProviderException;
import com.daesabu.meongcoach.auth.domain.exception.WithdrawnUserException;
import com.daesabu.meongcoach.user.application.UserQueryService;
import com.daesabu.meongcoach.user.application.UserRegisterService;
import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserStatus;
import com.daesabu.meongcoach.user.domain.exception.UserNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 제공자 호출(ID 토큰 검증·Apple revoke)과 JWT 서명은 외부 자원이라 스텁으로 바꾸고, 계정·토큰·회원 상태는 실제 DB로 검증한다.
 * 서비스는 프록시 없이 직접 조립하므로 트랜잭션 경계는 이 테스트의 검증 대상이 아니다.
 */
@DataJpaTest
class AuthenticationServiceTest {

	private static final Long UNREGISTERED_USER_ID = 999L;
	private static final String KAKAO_PROVIDER_ID = "3812345678";
	private static final String ID_TOKEN = "kakao-id-token";
	private static final SocialLoginRequest KAKAO_LOGIN = new SocialLoginRequest("KAKAO", ID_TOKEN);

	private static final Email EMAIL = new Email("review@meongcoach.com");
	private static final String PASSWORD = "meongcoach-review";
	// 검증 로직은 해시 강도와 무관하므로 테스트에서는 최소 강도로 해싱 비용을 줄인다
	private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(4);

	private static final RefreshTokenId STORED_TOKEN_ID = new RefreshTokenId("0f8fad5b-d9cb-469f-a165-70867728950e");
	private static final RefreshTokenId UNKNOWN_TOKEN_ID = new RefreshTokenId("7c9e6679-7425-40de-944b-e07fc1f90ae7");
	private static final String INVALID_TOKEN = "invalid";

	private static final String APPLE_CODE = "c1a2b3.0.abcd.efgh";
	private static final SocialAccountRegisterCommand APPLE_ACCOUNT = new SocialAccountRegisterCommand(
			SocialProvider.APPLE, "001234.abcdef", new Email("a@privaterelay.appleid.com"));
	private static final SocialAccountRegisterCommand KAKAO_ACCOUNT = new SocialAccountRegisterCommand(
			SocialProvider.KAKAO, KAKAO_PROVIDER_ID, new Email("k@kakao.com"));

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserProfileRepository userProfileRepository;

	@Autowired
	private SocialAccountRepository socialAccountRepository;

	@Autowired
	private EmailAccountRepository emailAccountRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private TestEntityManager entityManager;

	private final RecordingTokenRevoker tokenRevoker = new RecordingTokenRevoker();

	private AuthenticationService service;

	@BeforeEach
	void setUp() {
		StubTokenProvider tokenProvider = new StubTokenProvider();
		UserQueryService userFinder = new UserQueryService(userRepository);
		UserRegisterService userRegister = new UserRegisterService(userFinder, userRepository, userProfileRepository);
		AccountQueryService accountFinder = new AccountQueryService(emailAccountRepository, socialAccountRepository);
		AccountModifyService accountRegister = new AccountModifyService(socialAccountRepository, userRegister,
				emailAccountRepository);
		RefreshTokenQueryService refreshTokenFinder = new RefreshTokenQueryService(refreshTokenRepository);
		RefreshTokenModifyService refreshTokenRegister = new RefreshTokenModifyService(refreshTokenRepository,
				tokenProvider, refreshTokenFinder);
		AuthTokenProvideService authTokenProvider = new AuthTokenProvideService(tokenProvider, refreshTokenRegister);

		service = new AuthenticationService(accountFinder, accountRegister, PASSWORD_ENCODER::matches,
				authTokenProvider, tokenProvider, refreshTokenRegister, socialProfileReaders(),
				new SocialTokenRevokers(List.of(tokenRevoker)), refreshTokenFinder, userFinder, userRegister);
	}

	@Test
	void 소셜_계정으로_최초_로그인하면_회원과_소셜_계정이_함께_생성된다() {
		AuthToken token = service.socialLogin(KAKAO_LOGIN);

		assertThat(userRepository.count()).isEqualTo(1);
		assertThat(socialAccountRepository.count()).isEqualTo(1);
		assertThat(userRepository.findAll().getFirst().isOnboarding()).isTrue();
		assertThat(token.accessToken()).isNotBlank();
	}

	@Test
	void 이미_등록된_소셜_계정으로_재로그인하면_회원을_새로_만들지_않는다() {
		AuthToken first = service.socialLogin(KAKAO_LOGIN);

		AuthToken second = service.socialLogin(KAKAO_LOGIN);

		assertThat(userRepository.count()).isEqualTo(1);
		assertThat(socialAccountRepository.count()).isEqualTo(1);
		assertThat(second.accessToken()).isEqualTo(first.accessToken());
	}

	// 클라이언트가 제공자 표기의 대소문자를 맞추지 않아도 되게 한다
	@Test
	void 제공자를_소문자로_보내도_로그인된다() {
		AuthToken token = service.socialLogin(new SocialLoginRequest("kakao", ID_TOKEN));

		assertThat(token.accessToken()).isNotBlank();
		assertThat(socialAccountRepository.findByProviderAndProviderId(SocialProvider.KAKAO, KAKAO_PROVIDER_ID))
				.isPresent();
	}

	@Test
	void 지원하지_않는_제공자면_로그인할_수_없다() {
		assertThatThrownBy(() -> service.socialLogin(new SocialLoginRequest("naver", ID_TOKEN)))
				.isInstanceOf(UnsupportedSocialProviderException.class);
		assertThat(userRepository.count()).isZero();
	}

	@Test
	void 소셜_로그인하면_리프레시_토큰이_저장된다() {
		AuthToken token = service.socialLogin(KAKAO_LOGIN);

		User user = userRepository.findAll().getFirst();
		assertThat(refreshTokenRepository.findByTokenId(token.refreshTokenId()))
				.hasValueSatisfying(stored -> assertThat(stored.getUserId()).isEqualTo(user.getId()));
	}

	// 탈퇴는 자격증명을 지우므로 정상 흐름에서는 도달하지 않지만, 자격증명이 남은 탈퇴 회원에게 토큰을 내주지 않는다
	@Test
	void 탈퇴한_회원은_소셜_로그인할_수_없다() {
		service.socialLogin(KAKAO_LOGIN);
		userRepository.findAll().getFirst().withdraw();
		flushAndClear();

		assertThatThrownBy(() -> service.socialLogin(KAKAO_LOGIN))
				.isInstanceOf(WithdrawnUserException.class);
	}

	@Test
	void 이메일과_비밀번호가_일치하면_해당_회원의_토큰을_발급한다() {
		User user = persistEmailUser();

		AuthToken token = service.emailLogin(new EmailLoginRequest(EMAIL.address(), PASSWORD));

		assertThat(token.accessToken()).isEqualTo("access-" + user.getId());
		assertThat(refreshTokenRepository.findByTokenId(token.refreshTokenId()))
				.hasValueSatisfying(stored -> assertThat(stored.getUserId()).isEqualTo(user.getId()));
	}

	// 이메일 미존재와 비밀번호 불일치를 같은 예외로 응답해 계정 존재 여부를 드러내지 않는다
	@Test
	void 등록되지_않은_이메일이면_자격증명_오류를_던진다() {
		persistEmailUser();

		assertThatThrownBy(() -> service.emailLogin(new EmailLoginRequest("nobody@meongcoach.com", PASSWORD)))
				.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void 비밀번호가_틀리면_자격증명_오류를_던진다() {
		persistEmailUser();

		assertThatThrownBy(() -> service.emailLogin(new EmailLoginRequest(EMAIL.address(), "wrong-password")))
				.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void 이메일_형식이_올바르지_않으면_로그인할_수_없다() {
		assertThatThrownBy(() -> service.emailLogin(new EmailLoginRequest("not-an-email", PASSWORD)))
				.isInstanceOf(InvalidEmailException.class);
	}

	@Test
	void 탈퇴한_회원은_이메일_로그인할_수_없다() {
		User user = persistEmailUser();
		userRepository.findById(user.getId()).orElseThrow().withdraw();
		flushAndClear();

		assertThatThrownBy(() -> service.emailLogin(new EmailLoginRequest(EMAIL.address(), PASSWORD)))
				.isInstanceOf(WithdrawnUserException.class);
	}

	// 비밀번호 대조 뒤에 확인해야 탈퇴 여부가 비밀번호를 모르는 쪽에 드러나지 않는다
	@Test
	void 탈퇴한_회원이라도_비밀번호가_틀리면_자격증명_오류를_던진다() {
		User user = persistEmailUser();
		userRepository.findById(user.getId()).orElseThrow().withdraw();
		flushAndClear();

		assertThatThrownBy(() -> service.emailLogin(new EmailLoginRequest(EMAIL.address(), "wrong-password")))
				.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void 저장된_토큰으로_재발급하면_기존_토큰은_폐기되고_새_토큰이_저장된다() {
		User user = userRepository.save(User.registerUser());
		RefreshToken stored = persistToken(token(user, STORED_TOKEN_ID, LocalDateTime.now().plusDays(14)));

		AuthToken token = service.refresh(new TokenRefreshRequest(STORED_TOKEN_ID.value()));
		flushAndClear();

		RefreshToken revoked = refreshTokenRepository.findById(stored.getId()).orElseThrow();
		RefreshToken rotated = refreshTokenRepository.findByTokenId(token.refreshTokenId()).orElseThrow();
		assertThat(revoked.getRevokedAt()).isNotNull();
		assertThat(rotated.getUserId()).isEqualTo(user.getId());
		assertThat(rotated.getRevokedAt()).isNull();
		assertThat(token.accessToken()).isEqualTo("access-" + user.getId());
	}

	@Test
	void 저장되지_않은_토큰이면_재발급할_수_없다() {
		assertThatThrownBy(() -> service.refresh(new TokenRefreshRequest(UNKNOWN_TOKEN_ID.value())))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	void 이미_폐기된_토큰이면_재발급할_수_없다() {
		User user = userRepository.save(User.registerUser());
		RefreshToken revoked = token(user, STORED_TOKEN_ID, LocalDateTime.now().plusDays(14));
		revoked.revoke();
		persistToken(revoked);

		assertThatThrownBy(() -> service.refresh(new TokenRefreshRequest(STORED_TOKEN_ID.value())))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	void 만료된_토큰이면_재발급할_수_없다() {
		User user = userRepository.save(User.registerUser());
		persistToken(token(user, STORED_TOKEN_ID, LocalDateTime.now().minusMinutes(1)));

		assertThatThrownBy(() -> service.refresh(new TokenRefreshRequest(STORED_TOKEN_ID.value())))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	void 탈퇴한_회원의_토큰이면_재발급할_수_없다() {
		User user = userRepository.save(User.registerUser());
		persistToken(token(user, STORED_TOKEN_ID, LocalDateTime.now().plusDays(14)));
		userRepository.findById(user.getId()).orElseThrow().withdraw();
		flushAndClear();

		assertThatThrownBy(() -> service.refresh(new TokenRefreshRequest(STORED_TOKEN_ID.value())))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	void 서명_검증에_실패하면_재발급할_수_없다() {
		assertThatThrownBy(() -> service.refresh(new TokenRefreshRequest(INVALID_TOKEN)))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	void 로그아웃하면_토큰이_폐기된다() {
		User user = userRepository.save(User.registerUser());
		RefreshToken stored = persistToken(token(user, STORED_TOKEN_ID, LocalDateTime.now().plusDays(14)));

		service.logout(new LogoutRequest(STORED_TOKEN_ID.value()));
		flushAndClear();

		assertThat(refreshTokenRepository.findById(stored.getId()).orElseThrow().getRevokedAt()).isNotNull();
	}

	@Test
	void 저장되지_않은_토큰이면_로그아웃할_수_없다() {
		assertThatThrownBy(() -> service.logout(new LogoutRequest(UNKNOWN_TOKEN_ID.value())))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	void 탈퇴하면_회원_상태가_WITHDRAWN이_되고_행은_남는다() {
		Long userId = persistSocialUser(APPLE_ACCOUNT).getId();

		service.withdraw(userId, new WithdrawRequest(APPLE_CODE));
		flushAndClear();

		assertThat(userRepository.findById(userId).orElseThrow().getStatus()).isEqualTo(UserStatus.WITHDRAWN);
	}

	@Test
	void 탈퇴하면_소셜_계정과_이메일_계정_행이_삭제된다() {
		User user = persistSocialUser(APPLE_ACCOUNT);
		emailAccountRepository.save(EmailAccountFixture.create(user.getId(), EMAIL, "hashed"));
		flushAndClear();

		service.withdraw(user.getId(), new WithdrawRequest(APPLE_CODE));
		flushAndClear();

		assertThat(socialAccountRepository.findAllByUserId(user.getId())).isEmpty();
		assertThat(emailAccountRepository.findByEmail(EMAIL)).isEmpty();
	}

	// 애플 심사 요건: 탈퇴 후 같은 계정으로 다시 가입할 수 있어야 한다
	@Test
	void 탈퇴한_소셜_계정으로_다시_로그인하면_새_회원으로_가입된다() {
		service.socialLogin(KAKAO_LOGIN);
		Long withdrawnUserId = userRepository.findAll().getFirst().getId();
		service.withdraw(withdrawnUserId, null);
		flushAndClear();

		service.socialLogin(KAKAO_LOGIN);

		assertThat(userRepository.count()).isEqualTo(2);
		Long rejoinedUserId = socialAccountRepository.findByProviderAndProviderId(SocialProvider.KAKAO,
				KAKAO_PROVIDER_ID).orElseThrow().getUserId();
		assertThat(rejoinedUserId).isNotEqualTo(withdrawnUserId);
	}

	@Test
	void 탈퇴하면_회원의_살아있는_리프레시_토큰이_모두_폐기된다() {
		User user = persistSocialUser(APPLE_ACCOUNT);
		RefreshToken phone = persistToken(token(user, RefreshTokenId.generate(), LocalDateTime.now().plusDays(14)));
		RefreshToken tablet = persistToken(token(user, RefreshTokenId.generate(), LocalDateTime.now().plusDays(14)));

		service.withdraw(user.getId(), new WithdrawRequest(APPLE_CODE));
		flushAndClear();

		assertThat(refreshTokenRepository.findById(phone.getId()).orElseThrow().getRevokedAt()).isNotNull();
		assertThat(refreshTokenRepository.findById(tablet.getId()).orElseThrow().getRevokedAt()).isNotNull();
	}

	@Test
	void 없는_회원_ID로_탈퇴하면_예외를_던진다() {
		assertThatThrownBy(() -> service.withdraw(UNREGISTERED_USER_ID, null))
				.isInstanceOf(UserNotFoundException.class);
	}

	// 애플 심사 지침 5.1.1(v): Sign in with Apple 계정을 삭제할 때는 Apple 토큰을 revoke해야 한다
	@Test
	void revoker가_등록된_제공자_계정은_인가_코드로_revoke한_뒤_탈퇴한다() {
		Long userId = persistSocialUser(APPLE_ACCOUNT).getId();

		service.withdraw(userId, new WithdrawRequest(APPLE_CODE));
		flushAndClear();

		assertThat(tokenRevoker.revokedCode()).isEqualTo(APPLE_CODE);
		assertThat(userRepository.findById(userId).orElseThrow().getStatus()).isEqualTo(UserStatus.WITHDRAWN);
	}

	@Test
	void revoke가_실패하면_탈퇴되지_않는다() {
		Long userId = persistSocialUser(APPLE_ACCOUNT).getId();
		tokenRevoker.failWith(new InvalidAppleAuthorizationCodeException());

		assertThatThrownBy(() -> service.withdraw(userId, new WithdrawRequest(APPLE_CODE)))
				.isInstanceOf(InvalidAppleAuthorizationCodeException.class);
		flushAndClear();

		assertThat(userRepository.findById(userId).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(socialAccountRepository.findAllByUserId(userId)).hasSize(1);
	}

	// Apple 계정 회원만 본문을 보내므로 나머지 회원의 탈퇴 요청에는 본문이 없다
	@Test
	void revoker가_없는_제공자_계정만_있는_회원은_본문_없이_탈퇴할_수_있다() {
		Long userId = persistSocialUser(KAKAO_ACCOUNT).getId();

		service.withdraw(userId, null);
		flushAndClear();

		assertThat(tokenRevoker.revokedCode()).isNull();
		assertThat(userRepository.findById(userId).orElseThrow().getStatus()).isEqualTo(UserStatus.WITHDRAWN);
	}

	// SocialProfileReaders가 모든 제공자의 리더를 요구하므로 제공자마다 스텁을 넣는다
	private static SocialProfileReaders socialProfileReaders() {
		List<SocialProfileReader> readers = List.of(SocialProvider.values()).stream()
				.<SocialProfileReader>map(StubSocialProfileReader::new)
				.toList();
		return new SocialProfileReaders(readers);
	}

	private User persistEmailUser() {
		User user = userRepository.save(User.registerUser());
		String passwordHash = PASSWORD_ENCODER.encode(PASSWORD);
		emailAccountRepository.save(EmailAccountFixture.create(user.getId(), EMAIL, passwordHash));
		flushAndClear();
		return user;
	}

	private User persistSocialUser(SocialAccountRegisterCommand command) {
		User user = userRepository.save(User.registerUser());
		socialAccountRepository.save(SocialAccount.register(user.getId(), command));
		flushAndClear();
		return user;
	}

	private static RefreshToken token(User owner, RefreshTokenId tokenId, LocalDateTime expiresAt) {
		return RefreshToken.register(owner.getId(), new RefreshTokenRegisterCommand(tokenId, expiresAt));
	}

	private RefreshToken persistToken(RefreshToken token) {
		entityManager.persistAndFlush(token);
		entityManager.clear();
		return token;
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}

	private static class StubSocialProfileReader implements SocialProfileReader {

		private final SocialProvider provider;

		StubSocialProfileReader(SocialProvider provider) {
			this.provider = provider;
		}

		@Override
		public SocialProvider provider() {
			return provider;
		}

		@Override
		public SocialProfile read(String credential) {
			return new SocialProfile(provider, KAKAO_PROVIDER_ID, new Email("a@b.com"));
		}
	}

	// 서명 검증 대신 제시된 문자열을 그대로 jti로 취급해 저장 이력 검증만 남긴다.
	// 같은 회원이 여러 번 로그인해도 jti 유니크 제약에 걸리지 않도록 발급할 때마다 새 값을 만든다
	private static class StubTokenProvider implements TokenProvider {

		@Override
		public AuthToken issue(Long userId) {
			RefreshTokenId tokenId = RefreshTokenId.generate();
			return new AuthToken(userId, "access-" + userId, "refresh-" + userId, tokenId,
					LocalDateTime.now().plusDays(14));
		}

		@Override
		public RefreshTokenId extractTokenId(String refreshToken) {
			if (INVALID_TOKEN.equals(refreshToken)) {
				throw new InvalidRefreshTokenException();
			}
			return new RefreshTokenId(refreshToken);
		}
	}

	private static class RecordingTokenRevoker implements SocialTokenRevoker {

		private String revokedCode;
		private RuntimeException failure;

		@Override
		public SocialProvider provider() {
			return SocialProvider.APPLE;
		}

		@Override
		public void revoke(String authorizationCode) {
			if (failure != null) {
				throw failure;
			}
			this.revokedCode = authorizationCode;
		}

		String revokedCode() {
			return revokedCode;
		}

		void failWith(RuntimeException failure) {
			this.failure = failure;
		}
	}
}
