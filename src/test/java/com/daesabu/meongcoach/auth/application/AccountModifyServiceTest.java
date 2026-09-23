package com.daesabu.meongcoach.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.auth.application.provided.dto.SocialAccountRegisterRequest;
import com.daesabu.meongcoach.auth.application.required.EmailAccountRepository;
import com.daesabu.meongcoach.auth.application.required.SocialAccountRepository;
import com.daesabu.meongcoach.auth.domain.Email;
import com.daesabu.meongcoach.auth.domain.EmailAccountFixture;
import com.daesabu.meongcoach.auth.domain.SocialProvider;
import com.daesabu.meongcoach.user.application.UserQueryService;
import com.daesabu.meongcoach.user.application.UserRegisterService;
import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class AccountModifyServiceTest {

	private static final SocialAccountRegisterRequest KAKAO_ACCOUNT = new SocialAccountRegisterRequest(
			SocialProvider.KAKAO, "3812345678", new Email("k@kakao.com"));
	private static final SocialAccountRegisterRequest APPLE_ACCOUNT = new SocialAccountRegisterRequest(
			SocialProvider.APPLE, "001234.abcdef", new Email("a@privaterelay.appleid.com"));

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserProfileRepository userProfileRepository;

	@Autowired
	private SocialAccountRepository socialAccountRepository;

	@Autowired
	private EmailAccountRepository emailAccountRepository;

	@Autowired
	private TestEntityManager entityManager;

	private AccountModifyService service;

	@BeforeEach
	void setUp() {
		UserRegisterService userRegister = new UserRegisterService(new UserQueryService(userRepository),
				userRepository, userProfileRepository);
		service = new AccountModifyService(socialAccountRepository, userRegister, emailAccountRepository);
	}

	@Test
	void 등록되지_않은_소셜_계정이면_회원과_소셜_계정을_함께_만든다() {
		Long userId = service.upsertSocialAccount(KAKAO_ACCOUNT);

		User user = userRepository.findById(userId).orElseThrow();
		assertThat(user.isOnboarding()).isTrue();
		assertThat(socialAccountRepository.findByProviderAndProviderId(SocialProvider.KAKAO, "3812345678"))
				.hasValueSatisfying(account -> assertThat(account.getUserId()).isEqualTo(userId));
	}

	@Test
	void 이미_등록된_소셜_계정이면_연결된_회원_ID를_반환하고_새로_만들지_않는다() {
		Long registeredUserId = service.upsertSocialAccount(KAKAO_ACCOUNT);

		Long userId = service.upsertSocialAccount(KAKAO_ACCOUNT);

		assertThat(userId).isEqualTo(registeredUserId);
		assertThat(userRepository.count()).isEqualTo(1);
		assertThat(socialAccountRepository.count()).isEqualTo(1);
	}

	@Test
	void 회원의_소셜_계정만_삭제하고_다른_회원의_계정은_남긴다() {
		Long userId = service.upsertSocialAccount(KAKAO_ACCOUNT);
		Long otherUserId = service.upsertSocialAccount(APPLE_ACCOUNT);

		service.deleteAllSocialAccounts(userId);
		flushAndClear();

		assertThat(socialAccountRepository.findAllByUserId(userId)).isEmpty();
		assertThat(socialAccountRepository.findAllByUserId(otherUserId)).hasSize(1);
	}

	@Test
	void 회원의_이메일_계정을_삭제한다() {
		Long userId = userRepository.save(User.registerUser()).getId();
		Email email = new Email("review@meongcoach.com");
		emailAccountRepository.save(EmailAccountFixture.create(userId, email, "hashed"));
		flushAndClear();

		service.deleteAllEmailAccounts(userId);
		flushAndClear();

		assertThat(emailAccountRepository.findByEmail(email)).isEmpty();
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
