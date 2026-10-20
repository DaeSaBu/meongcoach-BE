package com.daesabu.meongcoach.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.user.domain.exception.AlreadyOnboardedException;
import com.daesabu.meongcoach.user.domain.exception.InvalidMbtiException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UserTest {

	@Test
	void 온보딩_회원으로_등록하면_ACTIVE_상태의_ONBOARDING_USER가_생성된다() {
		User user = User.registerUser();

		assertThat(user.getRole()).isEqualTo(UserRole.ONBOARDING_USER);
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	void 등록_직후에는_프로필이_없다() {
		User user = User.registerUser();

		assertThat(user.getProfile()).isNull();
	}

	@Test
	void 온보딩을_완료하면_USER가_되고_프로필이_생성된다() {
		User user = User.registerUser();

		user.completeOnboarding(profileCommand("INFP"));

		assertThat(user.getRole()).isEqualTo(UserRole.USER);
		assertThat(user.getProfile().getNickname()).isEqualTo("멍멍이집사");
		assertThat(user.getProfile().getMbti()).isEqualTo(Mbti.INFP);
	}

	@Test
	void 이미_USER면_온보딩_완료에_실패한다() {
		User user = User.registerUser();
		user.completeOnboarding(profileCommand("INFP"));

		assertThatThrownBy(() -> user.completeOnboarding(profileCommand("INTJ")))
				.isInstanceOf(AlreadyOnboardedException.class);
	}

	@Test
	void 프로필_정보가_잘못되면_온보딩_회원으로_남는다() {
		User user = User.registerUser();

		assertThatThrownBy(() -> user.completeOnboarding(profileCommand("XXXX")))
				.isInstanceOf(InvalidMbtiException.class);
		assertThat(user.isOnboarding()).isTrue();
		assertThat(user.getProfile()).isNull();
	}

	@Test
	void 온보딩_회원은_온보딩이_필요하다() {
		User user = User.registerUser();

		assertThat(user.isOnboarding()).isTrue();
	}

	@Test
	void 정회원은_온보딩이_필요하지_않다() {
		User user = User.registerUser();
		user.completeOnboarding(profileCommand("INFP"));

		assertThat(user.isOnboarding()).isFalse();
	}

	@Test
	void 모든_역할은_같은_이름의_인가_어휘로_매핑된다() {
		for (UserRole role : UserRole.values()) {
			assertThat(role.authorityRole().name()).isEqualTo(role.name());
		}
	}

	@Test
	void 탈퇴하면_상태가_WITHDRAWN으로_변경된다() {
		User user = User.registerUser();

		user.withdraw();

		assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
	}

	@Test
	void 탈퇴하면_프로필이_제거된다() {
		User user = User.registerUser();
		user.completeOnboarding(profileCommand("INFP"));

		user.withdraw();

		assertThat(user.getProfile()).isNull();
	}

	@Test
	void 등록_직후에는_탈퇴_상태가_아니다() {
		User user = User.registerUser();

		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	void 등록_직후에는_활성_회원이다() {
		User user = User.registerUser();

		assertThat(user.isActive()).isTrue();
	}

	@Test
	void 탈퇴하면_활성_회원이_아니다() {
		User user = User.registerUser();

		user.withdraw();

		assertThat(user.isActive()).isFalse();
	}

	private static UserProfileCreateCommand profileCommand(String mbti) {
		return new UserProfileCreateCommand("멍멍이집사", null, mbti, Set.of(), Set.of());
	}
}
