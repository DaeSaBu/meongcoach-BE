package com.daesabu.meongcoach.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.user.domain.exception.AlreadyOnboardedException;
import org.junit.jupiter.api.Test;

class UserTest {

	@Test
	void 온보딩_회원으로_등록하면_ACTIVE_상태의_ONBOARDING_USER가_생성된다() {
		User user = User.registerOnboardingUser();

		assertThat(user.getRole()).isEqualTo(UserRole.ONBOARDING_USER);
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	void 온보딩_회원을_승격하면_USER가_된다() {
		User user = User.registerOnboardingUser();

		user.promoteToUser();

		assertThat(user.getRole()).isEqualTo(UserRole.USER);
	}

	@Test
	void 이미_USER면_승격에_실패한다() {
		User user = User.registerOnboardingUser();
		user.promoteToUser();

		assertThatThrownBy(user::promoteToUser)
				.isInstanceOf(AlreadyOnboardedException.class);
	}

	@Test
	void 온보딩_회원은_온보딩이_필요하다() {
		User user = User.registerOnboardingUser();

		assertThat(user.isOnboarding()).isTrue();
	}

	@Test
	void 정회원은_온보딩이_필요하지_않다() {
		User user = User.registerOnboardingUser();
		user.promoteToUser();

		assertThat(user.isOnboarding()).isFalse();
	}

	// 인가 어휘 매핑이 잘못되면(예: USER에 ONBOARDING_USER 어휘) 인가 규칙 전체가 어긋나므로 선언부를 검증한다
	@Test
	void 모든_역할은_같은_이름의_인가_어휘로_매핑된다() {
		for (UserRole role : UserRole.values()) {
			assertThat(role.authorityRole().name()).isEqualTo(role.name());
		}
	}

	@Test
	void 탈퇴하면_상태가_WITHDRAWN으로_변경된다() {
		User user = User.registerOnboardingUser();

		user.withdraw();

		assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
		assertThat(user.isWithdrawn()).isTrue();
	}

	@Test
	void 등록_직후에는_탈퇴_상태가_아니다() {
		User user = User.registerOnboardingUser();

		assertThat(user.isWithdrawn()).isFalse();
	}
}
