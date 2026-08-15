package com.daesabu.meongcoach.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.shared.security.AuthorityRole;
import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.provided.RegisteredUserChecker;
import com.daesabu.meongcoach.user.application.required.TokenProvider;
import com.daesabu.meongcoach.user.domain.exception.InvalidRefreshTokenException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("토큰 재발급 서비스")
class TokenRefreshServiceTest {

	@Test
	@DisplayName("리프레시 토큰의 회원으로 새 토큰을 발급한다")
	void refreshIssuesNewTokenForTokenOwner() {
		TokenRefreshService service = new TokenRefreshService(new StubTokenProvider(7L), checker(true));

		AuthToken token = service.refresh("refresh-token");

		assertThat(token.accessToken()).isEqualTo("access-7");
		assertThat(token.refreshToken()).isEqualTo("refresh-7");
	}

	@Test
	@DisplayName("유효하지 않은 리프레시 토큰이면 예외를 그대로 전파한다")
	void refreshPropagatesInvalidTokenException() {
		TokenRefreshService service = new TokenRefreshService(new StubTokenProvider(null), checker(true));

		assertThatThrownBy(() -> service.refresh("invalid"))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	@DisplayName("등록되지 않은 회원이면 재발급하지 않는다")
	void refreshRejectsUnregisteredUser() {
		TokenRefreshService service = new TokenRefreshService(new StubTokenProvider(7L), checker(false));

		assertThatThrownBy(() -> service.refresh("refresh-token"))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	private static RegisteredUserChecker checker(boolean registered) {
		return new RegisteredUserChecker() {
			@Override
			public boolean isRegistered(Long userId) {
				return registered;
			}

			@Override
			public Optional<AuthorityRole> findRole(Long userId) {
				if (registered) {
					return Optional.of(AuthorityRole.MEMBER);
				}
				return Optional.empty();
			}
		};
	}

	private static class StubTokenProvider implements TokenProvider {

		private final Long userId;

		StubTokenProvider(Long userId) {
			this.userId = userId;
		}

		@Override
		public AuthToken issue(Long userId) {
			return new AuthToken("access-" + userId, "refresh-" + userId);
		}

		@Override
		public Long extractUserId(String refreshToken) {
			if (userId == null) {
				throw new InvalidRefreshTokenException();
			}
			return userId;
		}
	}
}
