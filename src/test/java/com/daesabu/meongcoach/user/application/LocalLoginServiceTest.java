package com.daesabu.meongcoach.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.provided.LoginResult;
import com.daesabu.meongcoach.user.application.required.LocalAccountRepository;
import com.daesabu.meongcoach.user.application.required.RefreshTokenRepository;
import com.daesabu.meongcoach.user.application.required.TokenProvider;
import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.LocalAccount;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfile;
import com.daesabu.meongcoach.user.domain.command.LocalAccountCreateCommand;
import com.daesabu.meongcoach.user.domain.command.UserProfileCreateCommand;
import com.daesabu.meongcoach.user.domain.exception.InvalidCredentialsException;
import com.daesabu.meongcoach.user.domain.exception.InvalidEmailException;
import com.daesabu.meongcoach.user.domain.exception.WithdrawnUserException;
import com.daesabu.meongcoach.user.domain.vo.Email;
import com.daesabu.meongcoach.user.domain.vo.RefreshTokenId;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@DataJpaTest
class LocalLoginServiceTest {

	private static final String EMAIL = "review@meongcoach.com";
	private static final String PASSWORD = "meongcoach-review";
	private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2026, 9, 16, 12, 0);

	// 검증 로직은 해시 강도와 무관하므로 테스트에서는 최소 강도로 해싱 비용을 줄인다
	private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(4);

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private LocalAccountRepository localAccountRepository;

	@Autowired
	private UserProfileRepository userProfileRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private TestEntityManager entityManager;

	private LocalLoginService service;

	private User user;

	@BeforeEach
	void setUp() {
		service = new LocalLoginService(localAccountRepository, userProfileRepository,
				new AuthTokenIssueService(new StubTokenProvider(), refreshTokenRepository), PASSWORD_ENCODER);
		user = userRepository.save(User.registerOnboardingMember());
		String passwordHash = PASSWORD_ENCODER.encode(PASSWORD);
		localAccountRepository.save(
				LocalAccount.create(user, new LocalAccountCreateCommand(new Email(EMAIL), passwordHash)));
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
				.hasValueSatisfying(stored -> assertThat(stored.getUser().getId()).isEqualTo(user.getId()));
	}

	@Test
	void 프로필이_있으면_온보딩이_필요하지_않다() {
		UserProfile profile = UserProfile.create(user,
				new UserProfileCreateCommand("멍코치", null, null, "INTJ", "NONE", Set.of(), Set.of()));
		entityManager.persistAndFlush(profile);
		entityManager.clear();

		LoginResult result = service.login(EMAIL, PASSWORD);

		assertThat(result.needsOnboarding()).isFalse();
	}

	@Test
	void 등록되지_않은_이메일이면_자격증명_오류를_던진다() {
		assertThatThrownBy(() -> service.login("nobody@meongcoach.com", PASSWORD))
				.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void 비밀번호가_틀리면_자격증명_오류를_던진다() {
		assertThatThrownBy(() -> service.login(EMAIL, "wrong-password"))
				.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void 이메일_형식이_올바르지_않으면_InvalidEmailException을_던진다() {
		assertThatThrownBy(() -> service.login("not-an-email", PASSWORD))
				.isInstanceOf(InvalidEmailException.class);
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
