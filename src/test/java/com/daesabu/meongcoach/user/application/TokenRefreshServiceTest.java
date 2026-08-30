package com.daesabu.meongcoach.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.shared.security.AuthorityRole;
import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.provided.RegisteredUserChecker;
import com.daesabu.meongcoach.user.application.required.TokenProvider;
import com.daesabu.meongcoach.user.domain.exception.InvalidRefreshTokenException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TokenRefreshServiceTest {

	@Test
	void 리프레시_토큰의_회원으로_새_토큰을_발급한다() {
		TokenRefreshService service = new TokenRefreshService(new StubTokenProvider(7L), checker(true));

		AuthToken token = service.refresh("refresh-token");

		assertThat(token.accessToken()).isEqualTo("access-7");
		assertThat(token.refreshToken()).isEqualTo("refresh-7");
	}

	@Test
	void 유효하지_않은_리프레시_토큰이면_예외를_그대로_전파한다() {
		TokenRefreshService service = new TokenRefreshService(new StubTokenProvider(null), checker(true));

		assertThatThrownBy(() -> service.refresh("invalid"))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	void 등록되지_않은_회원이면_재발급하지_않는다() {
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
