package com.daesabu.meongcoach.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.user.application.provided.UserWithdrawer;
import com.daesabu.meongcoach.user.application.required.LocalAccountRepository;
import com.daesabu.meongcoach.user.application.required.RefreshTokenRepository;
import com.daesabu.meongcoach.user.application.required.SocialAccountRepository;
import com.daesabu.meongcoach.user.application.required.SocialTokenRevoker;
import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.LocalAccount;
import com.daesabu.meongcoach.user.domain.RefreshToken;
import com.daesabu.meongcoach.user.domain.SocialAccount;
import com.daesabu.meongcoach.user.domain.SocialProvider;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfile;
import com.daesabu.meongcoach.user.domain.UserStatus;
import com.daesabu.meongcoach.user.domain.command.LocalAccountCreateCommand;
import com.daesabu.meongcoach.user.domain.command.SocialAccountLinkCommand;
import com.daesabu.meongcoach.user.domain.command.UserProfileCreateCommand;
import com.daesabu.meongcoach.user.domain.exception.InvalidAppleAuthorizationCodeException;
import com.daesabu.meongcoach.user.domain.exception.UserNotFoundException;
import com.daesabu.meongcoach.user.domain.vo.Email;
import com.daesabu.meongcoach.user.domain.vo.RefreshTokenId;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Apple 토큰 revoke는 외부 호출이라 기록용 스텁으로 바꿔 호출 여부·전달 값·실패 시 롤백만 본다.
 * 실제 Apple 요청 구성은 AppleSocialTokenRevokerTest가 맡는다.
 */
@DataJpaTest
@Import({UserWithdrawService.class, SocialUserRegisterService.class, UserWithdrawServiceTest.StubConfig.class})
class UserWithdrawServiceTest {

	private static final Long UNREGISTERED_USER_ID = 999L;
	private static final String APPLE_CODE = "c1a2b3.0.abcd.efgh";
	private static final SocialAccountLinkCommand APPLE_ACCOUNT =
			new SocialAccountLinkCommand(SocialProvider.APPLE, "001234.abcdef", "a@privaterelay.appleid.com");
	private static final SocialAccountLinkCommand KAKAO_ACCOUNT =
			new SocialAccountLinkCommand(SocialProvider.KAKAO, "3812345678", "k@kakao.com");

	@Autowired
	private UserWithdrawer userWithdrawer;

	@Autowired
	private RecordingTokenRevoker tokenRevoker;

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
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private TestEntityManager entityManager;

	@BeforeEach
	void resetRevoker() {
		tokenRevoker.reset();
	}

	@Test
	void 탈퇴하면_회원_상태가_WITHDRAWN이_되고_행은_남는다() {
		Long userId = persistSocialMember().getId();

		userWithdrawer.withdraw(userId, APPLE_CODE);
		flushAndClear();

		User withdrawn = userRepository.findById(userId).orElseThrow();
		assertThat(withdrawn.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
	}

	@Test
	void 탈퇴하면_소셜_계정과_프로필_행이_삭제된다() {
		User user = persistSocialMember();
		userProfileRepository.save(UserProfile.create(user, profileCommand()));
		flushAndClear();

		userWithdrawer.withdraw(user.getId(), APPLE_CODE);
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

		userWithdrawer.withdraw(user.getId(), null);
		flushAndClear();

		assertThat(localAccountRepository.findByEmail(email)).isEmpty();
		assertThat(userRepository.findById(user.getId())).isPresent();
	}

	@Test
	void 온보딩_미완료_회원도_탈퇴할_수_있다() {
		Long userId = persistSocialMember().getId();

		userWithdrawer.withdraw(userId, APPLE_CODE);
		flushAndClear();

		User withdrawn = userRepository.findById(userId).orElseThrow();
		assertThat(withdrawn.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
		assertThat(userProfileRepository.existsById(userId)).isFalse();
	}

	// 애플 심사 요건: 탈퇴 후 같은 계정으로 다시 가입할 수 있어야 한다
	@Test
	void 탈퇴한_소셜_계정으로_다시_로그인하면_새_회원으로_가입된다() {
		Long withdrawnUserId = persistSocialMember().getId();
		userWithdrawer.withdraw(withdrawnUserId, APPLE_CODE);
		flushAndClear();

		User rejoined = socialUserRegisterService.findOrRegister(APPLE_ACCOUNT);

		assertThat(rejoined.getId()).isNotEqualTo(withdrawnUserId);
		assertThat(rejoined.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(userRepository.count()).isEqualTo(2);
	}

	@Test
	void 탈퇴하면_회원의_살아있는_리프레시_토큰이_모두_폐기된다() {
		User user = persistSocialMember();
		RefreshToken phone = persistToken(RefreshToken.issue(user, RefreshTokenId.generate(), LocalDateTime.now().plusDays(14)));
		RefreshToken tablet = persistToken(RefreshToken.issue(user, RefreshTokenId.generate(), LocalDateTime.now().plusDays(14)));

		userWithdrawer.withdraw(user.getId(), APPLE_CODE);
		flushAndClear();

		assertThat(refreshTokenRepository.findById(phone.getId()).orElseThrow().getRevokedAt()).isNotNull();
		assertThat(refreshTokenRepository.findById(tablet.getId()).orElseThrow().getRevokedAt()).isNotNull();
	}

	@Test
	void 탈퇴해도_이미_폐기된_토큰의_폐기_시각은_바뀌지_않는다() {
		User user = persistSocialMember();
		RefreshToken revoked = RefreshToken.issue(user, RefreshTokenId.generate(), LocalDateTime.now().plusDays(14));
		revoked.revoke();
		persistToken(revoked);
		LocalDateTime firstRevokedAt = refreshTokenRepository.findById(revoked.getId()).orElseThrow().getRevokedAt();

		userWithdrawer.withdraw(user.getId(), APPLE_CODE);
		flushAndClear();

		assertThat(refreshTokenRepository.findById(revoked.getId()).orElseThrow().getRevokedAt())
				.isEqualTo(firstRevokedAt);
	}

	@Test
	void 없는_회원_ID로_탈퇴하면_예외를_던진다() {
		assertThatThrownBy(() -> userWithdrawer.withdraw(UNREGISTERED_USER_ID, APPLE_CODE))
				.isInstanceOf(UserNotFoundException.class);
	}

	// 애플 심사 지침 5.1.1(v): Sign in with Apple 계정을 삭제할 때는 Apple 토큰을 revoke해야 한다
	@Test
	void revoker가_등록된_제공자_계정은_인가_코드로_revoke한_뒤_탈퇴한다() {
		Long userId = persistSocialMember().getId();

		userWithdrawer.withdraw(userId, APPLE_CODE);
		flushAndClear();

		assertThat(tokenRevoker.revokedCode()).isEqualTo(APPLE_CODE);
		assertThat(userRepository.findById(userId).orElseThrow().getStatus()).isEqualTo(UserStatus.WITHDRAWN);
	}

	@Test
	void revoke가_실패하면_탈퇴되지_않는다() {
		Long userId = persistSocialMember().getId();
		tokenRevoker.failWith(new InvalidAppleAuthorizationCodeException());

		assertThatThrownBy(() -> userWithdrawer.withdraw(userId, APPLE_CODE))
				.isInstanceOf(InvalidAppleAuthorizationCodeException.class);
		flushAndClear();

		assertThat(userRepository.findById(userId).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(socialAccountRepository.findByProviderAndProviderId(APPLE_ACCOUNT.provider(), APPLE_ACCOUNT.providerId()))
				.isPresent();
	}

	@Test
	void revoker가_없는_제공자_계정만_있는_회원은_인가_코드가_있어도_revoke하지_않고_탈퇴한다() {
		User user = userRepository.save(User.registerOnboardingMember());
		socialAccountRepository.save(SocialAccount.link(user, KAKAO_ACCOUNT));
		flushAndClear();

		userWithdrawer.withdraw(user.getId(), APPLE_CODE);
		flushAndClear();

		assertThat(tokenRevoker.revokedCode()).isNull();
		assertThat(userRepository.findById(user.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.WITHDRAWN);
	}

	private User persistSocialMember() {
		User user = userRepository.save(User.registerOnboardingMember());
		socialAccountRepository.save(SocialAccount.link(user, APPLE_ACCOUNT));
		flushAndClear();
		return user;
	}

	private RefreshToken persistToken(RefreshToken token) {
		entityManager.persistAndFlush(token);
		entityManager.clear();
		return token;
	}

	private UserProfileCreateCommand profileCommand() {
		return new UserProfileCreateCommand("멍멍이집사", null, null, "INTJ", "FEMALE", Set.of(), Set.of());
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}

	@TestConfiguration
	static class StubConfig {

		@Bean
		RecordingTokenRevoker tokenRevoker() {
			return new RecordingTokenRevoker();
		}
	}

	static class RecordingTokenRevoker implements SocialTokenRevoker {

		private String revokedCode;
		private RuntimeException failure;

		@Override
		public SocialProvider provider() {
			return SocialProvider.APPLE;
		}

		@Override
		public void revoke(String authorizationCode) {
			if (failure != null) {
				throw failure;
			}
			this.revokedCode = authorizationCode;
		}

		String revokedCode() {
			return revokedCode;
		}

		void failWith(RuntimeException failure) {
			this.failure = failure;
		}

		void reset() {
			this.revokedCode = null;
			this.failure = null;
		}
	}
}
