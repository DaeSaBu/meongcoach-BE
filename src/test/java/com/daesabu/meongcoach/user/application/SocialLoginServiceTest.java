package com.daesabu.meongcoach.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.provided.LoginResult;
import com.daesabu.meongcoach.user.application.required.RefreshTokenRepository;
import com.daesabu.meongcoach.user.application.required.SocialAccountRepository;
import com.daesabu.meongcoach.user.application.required.SocialProfileReader;
import com.daesabu.meongcoach.user.application.required.TokenProvider;
import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.SocialProvider;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfile;
import com.daesabu.meongcoach.user.domain.UserStatus;
import com.daesabu.meongcoach.user.domain.command.SocialAccountLinkCommand;
import com.daesabu.meongcoach.user.domain.command.UserProfileCreateCommand;
import com.daesabu.meongcoach.user.domain.exception.UnsupportedSocialProviderException;
import com.daesabu.meongcoach.user.domain.exception.WithdrawnUserException;
import com.daesabu.meongcoach.user.domain.vo.RefreshTokenId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class SocialLoginServiceTest {

	private static final String PROVIDER_ID = "3812345678";
	private static final String CREDENTIAL = "kakao-access-token";
	private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2026, 9, 16, 12, 0);

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SocialAccountRepository socialAccountRepository;

	@Autowired
	private UserProfileRepository userProfileRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private TestEntityManager entityManager;

	private SocialLoginService service;

	@BeforeEach
	void setUp() {
		service = socialLoginService(new StubSocialProfileReader(PROVIDER_ID, "a@b.com"));
	}

	@Test
	void 최초_로그인하면_회원과_소셜_계정이_함께_생성된다() {
		LoginResult result = service.login(SocialProvider.KAKAO, CREDENTIAL);

		assertThat(userRepository.count()).isEqualTo(1);
		assertThat(socialAccountRepository.count()).isEqualTo(1);
		assertThat(result.token().accessToken()).isNotBlank();
		assertThat(result.needsOnboarding()).isTrue();
	}

	@Test
	void 이미_연동된_계정으로_재로그인하면_회원을_새로_만들지_않는다() {
		LoginResult first = service.login(SocialProvider.KAKAO, CREDENTIAL);

		LoginResult second = service.login(SocialProvider.KAKAO, CREDENTIAL);

		assertThat(userRepository.count()).isEqualTo(1);
		assertThat(socialAccountRepository.count()).isEqualTo(1);
		assertThat(second.token().accessToken()).isEqualTo(first.token().accessToken());
	}

	@Test
	void 프로필이_있으면_온보딩이_필요하지_않다() {
		service.login(SocialProvider.KAKAO, CREDENTIAL);
		User user = userRepository.findAll().getFirst();
		UserProfile profile = UserProfile.create(user,
				new UserProfileCreateCommand("멍코치", null, null, "INTJ", "NONE", Set.of(), Set.of()));
		entityManager.persistAndFlush(profile);
		entityManager.clear();

		LoginResult result = service.login(SocialProvider.KAKAO, CREDENTIAL);

		assertThat(result.needsOnboarding()).isFalse();
	}

	@Test
	void 탈퇴한_회원은_로그인할_수_없다() {
		service.login(SocialProvider.KAKAO, CREDENTIAL);
		User user = userRepository.findAll().getFirst();
		user.withdraw();
		entityManager.flush();
		entityManager.clear();

		assertThatThrownBy(() -> service.login(SocialProvider.KAKAO, CREDENTIAL))
				.isInstanceOf(WithdrawnUserException.class);
		assertThat(userRepository.findAll().getFirst().getStatus()).isEqualTo(UserStatus.WITHDRAWN);
	}

	@Test
	void 로그인하면_리프레시_토큰이_저장된다() {
		LoginResult result = service.login(SocialProvider.KAKAO, CREDENTIAL);

		User user = userRepository.findAll().getFirst();
		assertThat(refreshTokenRepository.findByTokenId(result.token().refreshTokenId()))
				.hasValueSatisfying(stored -> assertThat(stored.getUser().getId()).isEqualTo(user.getId()));
	}

	@Test
	void 구현체가_없는_제공자로는_로그인할_수_없다() {
		assertThatThrownBy(() -> service.login(SocialProvider.GOOGLE, CREDENTIAL))
				.isInstanceOf(UnsupportedSocialProviderException.class);
	}

	private SocialLoginService socialLoginService(SocialProfileReader reader) {
		return new SocialLoginService(List.of(reader),
				new SocialUserRegisterService(userRepository, socialAccountRepository, userProfileRepository),
				new AuthTokenIssueService(new StubTokenProvider(), refreshTokenRepository));
	}

	private static class StubSocialProfileReader implements SocialProfileReader {

		private final String providerId;
		private final String email;

		StubSocialProfileReader(String providerId, String email) {
			this.providerId = providerId;
			this.email = email;
		}

		@Override
		public SocialProvider provider() {
			return SocialProvider.KAKAO;
		}

		@Override
		public SocialAccountLinkCommand read(String credential) {
			return new SocialAccountLinkCommand(SocialProvider.KAKAO, providerId, email);
		}
	}

	private static class StubTokenProvider implements TokenProvider {

		// 같은 회원이 여러 번 로그인해도 jti 유니크 제약에 걸리지 않도록 매번 새 값을 만든다
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
