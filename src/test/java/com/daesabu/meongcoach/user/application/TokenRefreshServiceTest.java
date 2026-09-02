package com.daesabu.meongcoach.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.required.RefreshTokenRepository;
import com.daesabu.meongcoach.user.application.required.TokenProvider;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.RefreshToken;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.exception.InvalidRefreshTokenException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(RegisteredUserCheckService.class)
class TokenRefreshServiceTest {

	private static final String STORED_TOKEN_ID = "jti-stored";
	private static final String INVALID_TOKEN = "invalid";

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private RegisteredUserCheckService registeredUserCheckService;

	@Autowired
	private TestEntityManager entityManager;

	private TokenRefreshService service;

	private User user;

	@BeforeEach
	void setUp() {
		StubTokenProvider tokenProvider = new StubTokenProvider();
		service = new TokenRefreshService(tokenProvider, refreshTokenRepository, registeredUserCheckService,
				new AuthTokenIssueService(tokenProvider, refreshTokenRepository));
		user = userRepository.save(User.registerOnboardingMember());
	}

	@Test
	void 저장된_토큰으로_재발급하면_기존_토큰은_폐기되고_새_토큰이_저장된다() {
		RefreshToken stored = persistToken(user, LocalDateTime.now().plusDays(14));

		AuthToken token = service.refresh(STORED_TOKEN_ID);

		entityManager.flush();
		entityManager.clear();
		RefreshToken revoked = refreshTokenRepository.findById(stored.getId()).orElseThrow();
		RefreshToken rotated = refreshTokenRepository.findByTokenId(token.refreshTokenId()).orElseThrow();
		assertThat(revoked.getRevokedAt()).isNotNull();
		assertThat(rotated.getUser().getId()).isEqualTo(user.getId());
		assertThat(rotated.getRevokedAt()).isNull();
		assertThat(token.accessToken()).isEqualTo("access-" + user.getId());
	}

	@Test
	void 저장되지_않은_토큰이면_재발급할_수_없다() {
		assertThatThrownBy(() -> service.refresh("jti-unknown"))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	void 이미_폐기된_토큰이면_재발급할_수_없다() {
		RefreshToken revoked = RefreshToken.issue(user, STORED_TOKEN_ID, LocalDateTime.now().plusDays(14));
		revoked.revoke();
		entityManager.persistAndFlush(revoked);
		entityManager.clear();

		assertThatThrownBy(() -> service.refresh(STORED_TOKEN_ID))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	void 만료된_토큰이면_재발급할_수_없다() {
		persistToken(user, LocalDateTime.now().minusMinutes(1));

		assertThatThrownBy(() -> service.refresh(STORED_TOKEN_ID))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	void 탈퇴한_회원의_토큰이면_재발급할_수_없다() {
		persistToken(user, LocalDateTime.now().plusDays(14));
		User withdrawn = userRepository.findById(user.getId()).orElseThrow();
		withdrawn.withdraw();
		entityManager.flush();
		entityManager.clear();

		assertThatThrownBy(() -> service.refresh(STORED_TOKEN_ID))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	void 서명_검증에_실패하면_재발급할_수_없다() {
		assertThatThrownBy(() -> service.refresh(INVALID_TOKEN))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	private RefreshToken persistToken(User owner, LocalDateTime expiresAt) {
		RefreshToken token = RefreshToken.issue(owner, STORED_TOKEN_ID, expiresAt);
		entityManager.persistAndFlush(token);
		entityManager.clear();
		return token;
	}

	// 서명 검증 대신 제시된 문자열을 그대로 jti로 취급해 저장 이력 검증만 남긴다
	private static class StubTokenProvider implements TokenProvider {

		@Override
		public AuthToken issue(Long userId) {
			String tokenId = UUID.randomUUID().toString();
			return new AuthToken("access-" + userId, "refresh-" + userId, tokenId, LocalDateTime.now().plusDays(14));
		}

		@Override
		public String extractTokenId(String refreshToken) {
			if (INVALID_TOKEN.equals(refreshToken)) {
				throw new InvalidRefreshTokenException();
			}
			return refreshToken;
		}
	}
}
