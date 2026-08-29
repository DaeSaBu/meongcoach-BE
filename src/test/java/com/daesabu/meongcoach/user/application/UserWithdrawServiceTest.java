package com.daesabu.meongcoach.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.user.application.provided.UserWithdrawer;
import com.daesabu.meongcoach.user.application.required.LocalAccountRepository;
import com.daesabu.meongcoach.user.application.required.SocialAccountRepository;
import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.LocalAccount;
import com.daesabu.meongcoach.user.domain.SocialAccount;
import com.daesabu.meongcoach.user.domain.SocialProvider;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfile;
import com.daesabu.meongcoach.user.domain.UserStatus;
import com.daesabu.meongcoach.user.domain.command.LocalAccountCreateCommand;
import com.daesabu.meongcoach.user.domain.command.SocialAccountLinkCommand;
import com.daesabu.meongcoach.user.domain.command.UserProfileCreateCommand;
import com.daesabu.meongcoach.user.domain.exception.UserNotFoundException;
import com.daesabu.meongcoach.user.domain.vo.Email;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({UserWithdrawService.class, SocialUserRegisterService.class})
class UserWithdrawServiceTest {

	private static final Long UNREGISTERED_USER_ID = 999L;
	private static final SocialAccountLinkCommand APPLE_ACCOUNT =
			new SocialAccountLinkCommand(SocialProvider.APPLE, "001234.abcdef", "a@privaterelay.appleid.com");

	@Autowired
	private UserWithdrawer userWithdrawer;

	@Autowired
	private SocialUserRegisterService socialUserRegisterService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SocialAccountRepository socialAccountRepository;

	@Autowired
	private LocalAccountRepository localAccountRepository;

	@Autowired
	private UserProfileRepository userProfileRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 탈퇴하면_회원_상태가_WITHDRAWN이_되고_행은_남는다() {
		Long userId = persistSocialMember().getId();

		userWithdrawer.withdraw(userId);
		flushAndClear();

		User withdrawn = userRepository.findById(userId).orElseThrow();
		assertThat(withdrawn.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
	}

	@Test
	void 탈퇴하면_소셜_계정과_프로필_행이_삭제된다() {
		User user = persistSocialMember();
		userProfileRepository.save(UserProfile.create(user, profileCommand()));
		flushAndClear();

		userWithdrawer.withdraw(user.getId());
		flushAndClear();

		assertThat(socialAccountRepository.findByProviderAndProviderId(APPLE_ACCOUNT.provider(), APPLE_ACCOUNT.providerId()))
				.isEmpty();
		assertThat(userProfileRepository.existsById(user.getId())).isFalse();
	}

	@Test
	void 탈퇴하면_로컬_계정_행이_삭제된다() {
		User user = userRepository.save(User.registerOnboardingMember());
		Email email = new Email("review@meongcoach.com");
		localAccountRepository.save(LocalAccount.create(user, new LocalAccountCreateCommand(email, "hashed")));
		flushAndClear();

		userWithdrawer.withdraw(user.getId());
		flushAndClear();

		assertThat(localAccountRepository.findByEmail(email)).isEmpty();
		assertThat(userRepository.findById(user.getId())).isPresent();
	}

	@Test
	void 온보딩_미완료_회원도_탈퇴할_수_있다() {
		Long userId = persistSocialMember().getId();

		userWithdrawer.withdraw(userId);
		flushAndClear();

		User withdrawn = userRepository.findById(userId).orElseThrow();
		assertThat(withdrawn.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
		assertThat(userProfileRepository.existsById(userId)).isFalse();
	}

	// 애플 심사 요건: 탈퇴 후 같은 계정으로 다시 가입할 수 있어야 한다
	@Test
	void 탈퇴한_소셜_계정으로_다시_로그인하면_새_회원으로_가입된다() {
		Long withdrawnUserId = persistSocialMember().getId();
		userWithdrawer.withdraw(withdrawnUserId);
		flushAndClear();

		User rejoined = socialUserRegisterService.findOrRegister(APPLE_ACCOUNT);

		assertThat(rejoined.getId()).isNotEqualTo(withdrawnUserId);
		assertThat(rejoined.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(userRepository.count()).isEqualTo(2);
	}

	@Test
	void 없는_회원_ID로_탈퇴하면_예외를_던진다() {
		assertThatThrownBy(() -> userWithdrawer.withdraw(UNREGISTERED_USER_ID))
				.isInstanceOf(UserNotFoundException.class);
	}

	private User persistSocialMember() {
		User user = userRepository.save(User.registerOnboardingMember());
		socialAccountRepository.save(SocialAccount.link(user, APPLE_ACCOUNT));
		flushAndClear();
		return user;
	}

	private UserProfileCreateCommand profileCommand() {
		return new UserProfileCreateCommand("멍멍이집사", null, null, "INTJ", "FEMALE", Set.of(), Set.of());
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
