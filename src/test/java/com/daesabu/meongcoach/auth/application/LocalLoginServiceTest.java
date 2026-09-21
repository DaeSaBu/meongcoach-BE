package com.daesabu.meongcoach.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.auth.application.provided.AuthToken;
import com.daesabu.meongcoach.auth.application.provided.LoginResult;
import com.daesabu.meongcoach.auth.application.required.LocalAccountRepository;
import com.daesabu.meongcoach.auth.application.required.RefreshTokenRepository;
import com.daesabu.meongcoach.auth.application.required.TokenProvider;
import com.daesabu.meongcoach.auth.domain.Email;
import com.daesabu.meongcoach.auth.domain.LocalAccount;
import com.daesabu.meongcoach.auth.domain.LocalAccountCreateCommand;
import com.daesabu.meongcoach.auth.domain.RefreshTokenId;
import com.daesabu.meongcoach.auth.domain.exception.InvalidCredentialsException;
import com.daesabu.meongcoach.auth.domain.exception.WithdrawnUserException;
import com.daesabu.meongcoach.user.application.RegisteredUserCheckService;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@DataJpaTest
@Import(RegisteredUserCheckService.class)
class LocalLoginServiceTest {

	private static final Email EMAIL = new Email("review@meongcoach.com");
	private static final String PASSWORD = "meongcoach-review";
	private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2026, 9, 16, 12, 0);

	// 검증 로직은 해시 강도와 무관하므로 테스트에서는 최소 강도로 해싱 비용을 줄인다
	private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(4);

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RegisteredUserCheckService registeredUserCheckService;

	@Autowired
	private LocalAccountRepository localAccountRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private TestEntityManager entityManager;

	private LocalLoginService service;

	private User user;

	@BeforeEach
	void setUp() {
		service = new LocalLoginService(localAccountRepository,
				new AuthTokenIssueService(new StubTokenProvider(), refreshTokenRepository), PASSWORD_ENCODER::matches,
				registeredUserCheckService);
		user = userRepository.save(User.registerUser());
		String passwordHash = PASSWORD_ENCODER.encode(PASSWORD);
		localAccountRepository.save(
				LocalAccount.create(user.getId(), new LocalAccountCreateCommand(EMAIL, passwordHash)));
		entityManager.flush();
		entityManager.clear();
	}

	@Test
	void 이메일과_비밀번호가_일치하면_해당_회원의_토큰을_발급한다() {
		LoginResult result = service.login(EMAIL, PASSWORD);

		assertThat(result.token().accessToken()).isEqualTo("access-" + user.getId());
		assertThat(result.needsOnboarding()).isTrue();
	}

	@Test
	void 로그인하면_리프레시_토큰이_저장된다() {
		LoginResult result = service.login(EMAIL, PASSWORD);

		assertThat(refreshTokenRepository.findByTokenId(result.token().refreshTokenId()))
				.hasValueSatisfying(stored -> assertThat(stored.getUserId()).isEqualTo(user.getId()));
	}

	@Test
	void 정회원이면_온보딩이_필요하지_않다() {
		User promoted = userRepository.findById(user.getId()).orElseThrow();
		promoted.promoteToUser();
		entityManager.flush();
		entityManager.clear();

		LoginResult result = service.login(EMAIL, PASSWORD);

		assertThat(result.needsOnboarding()).isFalse();
	}

	@Test
	void 등록되지_않은_이메일이면_자격증명_오류를_던진다() {
		assertThatThrownBy(() -> service.login(new Email("nobody@meongcoach.com"), PASSWORD))
				.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void 비밀번호가_틀리면_자격증명_오류를_던진다() {
		assertThatThrownBy(() -> service.login(EMAIL, "wrong-password"))
				.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void 탈퇴한_회원은_로그인할_수_없다() {
		User withdrawn = userRepository.findById(user.getId()).orElseThrow();
		withdrawn.withdraw();
		entityManager.flush();
		entityManager.clear();

		assertThatThrownBy(() -> service.login(EMAIL, PASSWORD))
				.isInstanceOf(WithdrawnUserException.class);
	}

	private static class StubTokenProvider implements TokenProvider {

		@Override
		public AuthToken issue(Long userId) {
			RefreshTokenId tokenId = RefreshTokenId.generate();
			return new AuthToken("access-" + userId, "refresh-" + userId, tokenId, EXPIRES_AT);
		}

		@Override
		public RefreshTokenId extractTokenId(String refreshToken) {
			throw new UnsupportedOperationException();
		}
	}
}
