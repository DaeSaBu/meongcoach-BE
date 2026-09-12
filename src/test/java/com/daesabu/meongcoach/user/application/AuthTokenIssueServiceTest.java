package com.daesabu.meongcoach.user.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.required.LocalAccountRepository;
import com.daesabu.meongcoach.user.application.required.RefreshTokenRepository;
import com.daesabu.meongcoach.user.application.required.SocialAccountRepository;
import com.daesabu.meongcoach.user.application.required.TokenProvider;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.LocalAccount;
import com.daesabu.meongcoach.user.domain.RefreshToken;
import com.daesabu.meongcoach.user.domain.SocialAccount;
import com.daesabu.meongcoach.user.domain.SocialProvider;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.command.LocalAccountCreateCommand;
import com.daesabu.meongcoach.user.domain.command.SocialAccountLinkCommand;
import com.daesabu.meongcoach.user.domain.vo.Email;
import com.daesabu.meongcoach.user.domain.vo.RefreshTokenId;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class AuthTokenIssueServiceTest {

	private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2026, 9, 16, 12, 0);
	private static final String SOCIAL_EMAIL = "social@meongcoach.com";
	private static final String LOCAL_EMAIL = "review@meongcoach.com";

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private SocialAccountRepository socialAccountRepository;

	@Autowired
	private LocalAccountRepository localAccountRepository;

	private StubTokenProvider tokenProvider;

	private AuthTokenIssueService service;

	private User user;

	@BeforeEach
	void setUp() {
		tokenProvider = new StubTokenProvider();
		service = new AuthTokenIssueService(tokenProvider, refreshTokenRepository, socialAccountRepository,
				localAccountRepository);
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

	@Test
	void 소셜_계정의_이메일을_토큰_발급에_넘긴다() {
		linkSocialAccount(SocialProvider.KAKAO, "3812345678", SOCIAL_EMAIL);

		service.issue(user);

		assertThat(tokenProvider.issuedEmail).isEqualTo(SOCIAL_EMAIL);
	}

	@Test
	void 이메일_없는_소셜_계정만_있으면_null을_넘긴다() {
		linkSocialAccount(SocialProvider.KAKAO, "3812345678", null);

		service.issue(user);

		assertThat(tokenProvider.issuedEmail).isNull();
	}

	@Test
	void 이메일을_내려주지_않은_제공자가_섞여_있으면_이메일이_있는_계정을_고른다() {
		linkSocialAccount(SocialProvider.KAKAO, "3812345678", null);
		linkSocialAccount(SocialProvider.GOOGLE, "104738291", SOCIAL_EMAIL);

		service.issue(user);

		assertThat(tokenProvider.issuedEmail).isEqualTo(SOCIAL_EMAIL);
	}

	@Test
	void 로컬_계정의_이메일을_토큰_발급에_넘긴다() {
		localAccountRepository.save(
				LocalAccount.create(user, new LocalAccountCreateCommand(new Email(LOCAL_EMAIL), "hash")));

		service.issue(user);

		assertThat(tokenProvider.issuedEmail).isEqualTo(LOCAL_EMAIL);
	}

	@Test
	void 이메일을_가진_계정이_없으면_null을_넘긴다() {
		service.issue(user);

		assertThat(tokenProvider.issuedEmail).isNull();
	}

	private void linkSocialAccount(SocialProvider provider, String providerId, String email) {
		socialAccountRepository.save(
				SocialAccount.link(user, new SocialAccountLinkCommand(provider, providerId, email)));
	}

	private static class StubTokenProvider implements TokenProvider {

		private String issuedEmail;

		@Override
		public AuthToken issue(Long userId, String email) {
			issuedEmail = email;
			return new AuthToken("access-" + userId, "refresh-" + userId, RefreshTokenId.generate(), EXPIRES_AT);
		}

		@Override
		public RefreshTokenId extractTokenId(String refreshToken) {
			throw new UnsupportedOperationException();
		}
	}
}
