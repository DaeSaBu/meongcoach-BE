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
import com.daesabu.meongcoach.user.domain.vo.RefreshTokenId;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class LogoutServiceTest {

	private static final RefreshTokenId STORED_TOKEN_ID = new RefreshTokenId("0f8fad5b-d9cb-469f-a165-70867728950e");
	private static final RefreshTokenId UNKNOWN_TOKEN_ID = new RefreshTokenId("7c9e6679-7425-40de-944b-e07fc1f90ae7");
	private static final String INVALID_TOKEN = "invalid";

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private TestEntityManager entityManager;

	private LogoutService service;

	private User user;

	@BeforeEach
	void setUp() {
		service = new LogoutService(new StubTokenProvider(), refreshTokenRepository);
		user = userRepository.save(User.registerOnboardingMember());
	}

	@Test
	void 로그아웃하면_토큰이_폐기된다() {
		RefreshToken stored = persistToken(RefreshToken.issue(user, STORED_TOKEN_ID, LocalDateTime.now().plusDays(14)));

		service.logout(STORED_TOKEN_ID.value());

		entityManager.flush();
		entityManager.clear();
		RefreshToken revoked = refreshTokenRepository.findById(stored.getId()).orElseThrow();
		assertThat(revoked.getRevokedAt()).isNotNull();
	}

	@Test
	void 이미_폐기된_토큰을_로그아웃해도_처음_폐기_시각이_유지된다() {
		RefreshToken revoked = RefreshToken.issue(user, STORED_TOKEN_ID, LocalDateTime.now().plusDays(14));
		revoked.revoke();
		persistToken(revoked);
		LocalDateTime firstRevokedAt = refreshTokenRepository.findById(revoked.getId()).orElseThrow().getRevokedAt();

		service.logout(STORED_TOKEN_ID.value());

		entityManager.flush();
		entityManager.clear();
		RefreshToken stored = refreshTokenRepository.findById(revoked.getId()).orElseThrow();
		assertThat(stored.getRevokedAt()).isEqualTo(firstRevokedAt);
	}

	@Test
	void 저장되지_않은_토큰이면_로그아웃할_수_없다() {
		assertThatThrownBy(() -> service.logout(UNKNOWN_TOKEN_ID.value()))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	void 서명_검증에_실패하면_로그아웃할_수_없다() {
		assertThatThrownBy(() -> service.logout(INVALID_TOKEN))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	private RefreshToken persistToken(RefreshToken token) {
		entityManager.persistAndFlush(token);
		entityManager.clear();
		return token;
	}

	// 서명 검증 대신 제시된 문자열을 그대로 jti로 취급해 저장 이력 검증만 남긴다
	private static class StubTokenProvider implements TokenProvider {

		@Override
		public AuthToken issue(Long userId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public RefreshTokenId extractTokenId(String refreshToken) {
			if (INVALID_TOKEN.equals(refreshToken)) {
				throw new InvalidRefreshTokenException();
			}
			return new RefreshTokenId(refreshToken);
		}
	}
}
