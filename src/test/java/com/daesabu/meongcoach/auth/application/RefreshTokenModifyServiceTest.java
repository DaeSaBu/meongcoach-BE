package com.daesabu.meongcoach.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.auth.application.provided.dto.RefreshTokenRegisterRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.RefreshTokenRevokeRequest;
import com.daesabu.meongcoach.auth.application.required.RefreshTokenRepository;
import com.daesabu.meongcoach.auth.application.required.TokenProvider;
import com.daesabu.meongcoach.auth.domain.AuthToken;
import com.daesabu.meongcoach.auth.domain.RefreshToken;
import com.daesabu.meongcoach.auth.domain.RefreshTokenId;
import com.daesabu.meongcoach.auth.domain.RefreshTokenRegisterCommand;
import com.daesabu.meongcoach.auth.domain.exception.InvalidRefreshTokenException;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class RefreshTokenModifyServiceTest {

	private static final RefreshTokenId STORED_TOKEN_ID = new RefreshTokenId("0f8fad5b-d9cb-469f-a165-70867728950e");
	private static final RefreshTokenId UNKNOWN_TOKEN_ID = new RefreshTokenId("7c9e6679-7425-40de-944b-e07fc1f90ae7");
	private static final String INVALID_TOKEN = "invalid";
	private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2026, 9, 16, 12, 0);

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private TestEntityManager entityManager;

	private RefreshTokenModifyService service;

	private User user;

	@BeforeEach
	void setUp() {
		service = new RefreshTokenModifyService(refreshTokenRepository, new StubTokenProvider(),
				new RefreshTokenQueryService(refreshTokenRepository));
		user = userRepository.save(User.registerUser());
	}

	@Test
	void 등록하면_회원과_jti와_만료_시각으로_저장된다() {
		service.register(user.getId(), new RefreshTokenRegisterRequest(STORED_TOKEN_ID, EXPIRES_AT));

		RefreshToken stored = refreshTokenRepository.findByTokenId(STORED_TOKEN_ID).orElseThrow();
		assertThat(stored.getUserId()).isEqualTo(user.getId());
		assertThat(stored.getExpiresAt()).isEqualTo(EXPIRES_AT);
		assertThat(stored.getRevokedAt()).isNull();
	}

	@Test
	void 폐기하면_제시된_토큰에_폐기_시각이_기록된다() {
		RefreshToken stored = persistToken(token(STORED_TOKEN_ID));

		service.revoke(new RefreshTokenRevokeRequest(STORED_TOKEN_ID.value()));
		flushAndClear();

		assertThat(refreshTokenRepository.findById(stored.getId()).orElseThrow().getRevokedAt()).isNotNull();
	}

	// 같은 로그아웃 요청을 반복해도 결과가 같아야 한다
	@Test
	void 이미_폐기된_토큰을_다시_폐기해도_처음_폐기_시각이_유지된다() {
		RefreshToken revoked = token(STORED_TOKEN_ID);
		revoked.revoke();
		persistToken(revoked);
		LocalDateTime firstRevokedAt = refreshTokenRepository.findById(revoked.getId()).orElseThrow().getRevokedAt();

		service.revoke(new RefreshTokenRevokeRequest(STORED_TOKEN_ID.value()));
		flushAndClear();

		assertThat(refreshTokenRepository.findById(revoked.getId()).orElseThrow().getRevokedAt())
				.isEqualTo(firstRevokedAt);
	}

	@Test
	void 저장되지_않은_토큰은_폐기할_수_없다() {
		assertThatThrownBy(() -> service.revoke(new RefreshTokenRevokeRequest(UNKNOWN_TOKEN_ID.value())))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	void 서명_검증에_실패한_토큰은_폐기할_수_없다() {
		assertThatThrownBy(() -> service.revoke(new RefreshTokenRevokeRequest(INVALID_TOKEN)))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	void 회원의_살아있는_토큰을_모두_폐기하고_다른_회원의_토큰은_남긴다() {
		User other = userRepository.save(User.registerUser());
		RefreshToken phone = persistToken(token(RefreshTokenId.generate()));
		RefreshToken tablet = persistToken(token(RefreshTokenId.generate()));
		RefreshToken othersToken = persistToken(RefreshToken.register(other.getId(),
				new RefreshTokenRegisterCommand(RefreshTokenId.generate(), EXPIRES_AT)));

		service.revokeAllByUserId(user.getId());
		flushAndClear();

		assertThat(refreshTokenRepository.findById(phone.getId()).orElseThrow().getRevokedAt()).isNotNull();
		assertThat(refreshTokenRepository.findById(tablet.getId()).orElseThrow().getRevokedAt()).isNotNull();
		assertThat(refreshTokenRepository.findById(othersToken.getId()).orElseThrow().getRevokedAt()).isNull();
	}

	private RefreshToken token(RefreshTokenId tokenId) {
		return RefreshToken.register(user.getId(), new RefreshTokenRegisterCommand(tokenId, EXPIRES_AT));
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
