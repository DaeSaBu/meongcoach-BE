package com.daesabu.meongcoach.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.provided.SocialLoginResult;
import com.daesabu.meongcoach.user.application.required.SocialAccountRepository;
import com.daesabu.meongcoach.user.application.required.SocialProfileReader;
import com.daesabu.meongcoach.user.application.required.TokenProvider;
import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.SocialAccountLinkCommand;
import com.daesabu.meongcoach.user.domain.SocialProvider;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfile;
import com.daesabu.meongcoach.user.domain.UserStatus;
import com.daesabu.meongcoach.user.domain.exception.UnsupportedSocialProviderException;
import com.daesabu.meongcoach.user.domain.exception.WithdrawnUserException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
@DisplayName("소셜 로그인 서비스")
class SocialLoginServiceTest {

	private static final String PROVIDER_ID = "3812345678";
	private static final String CREDENTIAL = "kakao-access-token";

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SocialAccountRepository socialAccountRepository;

	@Autowired
	private UserProfileRepository userProfileRepository;

	@Autowired
	private TestEntityManager entityManager;

	private SocialLoginService service;

	@BeforeEach
	void setUp() {
		service = socialLoginService(new StubSocialProfileReader(PROVIDER_ID, "a@b.com"));
	}

	@Test
	@DisplayName("최초 로그인하면 회원과 소셜 계정이 함께 생성된다")
	void loginRegistersMemberOnFirstLogin() {
		SocialLoginResult result = service.login(SocialProvider.KAKAO, CREDENTIAL);

		assertThat(userRepository.count()).isEqualTo(1);
		assertThat(socialAccountRepository.count()).isEqualTo(1);
		assertThat(result.token().accessToken()).isNotBlank();
		assertThat(result.needsOnboarding()).isTrue();
	}

	@Test
	@DisplayName("이미 연동된 계정으로 재로그인하면 회원을 새로 만들지 않는다")
	void loginReusesUserOnSecondLogin() {
		SocialLoginResult first = service.login(SocialProvider.KAKAO, CREDENTIAL);

		SocialLoginResult second = service.login(SocialProvider.KAKAO, CREDENTIAL);

		assertThat(userRepository.count()).isEqualTo(1);
		assertThat(socialAccountRepository.count()).isEqualTo(1);
		assertThat(second.token().accessToken()).isEqualTo(first.token().accessToken());
	}

	@Test
	@DisplayName("프로필이 있으면 온보딩이 필요하지 않다")
	void loginReturnsOnboardingCompletedWhenProfileExists() {
		service.login(SocialProvider.KAKAO, CREDENTIAL);
		User user = userRepository.findAll().getFirst();
		entityManager.persistAndFlush(UserProfile.create(user, "멍코치"));
		entityManager.clear();

		SocialLoginResult result = service.login(SocialProvider.KAKAO, CREDENTIAL);

		assertThat(result.needsOnboarding()).isFalse();
	}

	@Test
	@DisplayName("탈퇴한 회원은 로그인할 수 없다")
	void loginFailsWhenUserIsWithdrawn() {
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
	@DisplayName("구현체가 없는 제공자로는 로그인할 수 없다")
	void loginFailsWhenProviderIsNotRegistered() {
		assertThatThrownBy(() -> service.login(SocialProvider.GOOGLE, CREDENTIAL))
				.isInstanceOf(UnsupportedSocialProviderException.class);
	}

	private SocialLoginService socialLoginService(SocialProfileReader reader) {
		return new SocialLoginService(List.of(reader),
				new SocialUserRegisterService(userRepository, socialAccountRepository, userProfileRepository),
				new StubTokenProvider());
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

		@Override
		public AuthToken issue(Long userId) {
			return new AuthToken("access-" + userId, "refresh-" + userId);
		}

		@Override
		public Long extractUserId(String refreshToken) {
			throw new UnsupportedOperationException();
		}
	}
}
