package com.daesabu.meongcoach.user.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.required.RefreshTokenRepository;
import com.daesabu.meongcoach.user.application.required.TokenProvider;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.RefreshToken;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.vo.RefreshTokenId;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class AuthTokenIssueServiceTest {

	private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2026, 9, 16, 12, 0);

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	private AuthTokenIssueService service;

	private User user;

	@BeforeEach
	void setUp() {
		service = new AuthTokenIssueService(new StubTokenProvider(), refreshTokenRepository);
		user = userRepository.save(User.registerOnboardingMember());
	}

	@Test
	void 발급하면_리프레시_토큰이_회원과_jti와_만료_시각으로_저장된다() {
		AuthToken token = service.issue(user);

		RefreshToken stored = refreshTokenRepository.findByTokenId(token.refreshTokenId()).orElseThrow();
		assertThat(stored.getUser().getId()).isEqualTo(user.getId());
		assertThat(stored.getTokenId()).isEqualTo(token.refreshTokenId());
		assertThat(stored.getExpiresAt()).isEqualTo(EXPIRES_AT);
		assertThat(stored.getRevokedAt()).isNull();
	}

	@Test
	void 발급한_토큰_쌍을_그대로_반환한다() {
		AuthToken token = service.issue(user);

		assertThat(token.accessToken()).isEqualTo("access-" + user.getId());
		assertThat(token.refreshToken()).isEqualTo("refresh-" + user.getId());
	}

	private static class StubTokenProvider implements TokenProvider {

		@Override
		public AuthToken issue(Long userId) {
			return new AuthToken("access-" + userId, "refresh-" + userId, RefreshTokenId.generate(), EXPIRES_AT);
		}

		@Override
		public RefreshTokenId extractTokenId(String refreshToken) {
			throw new UnsupportedOperationException();
		}
	}
}
