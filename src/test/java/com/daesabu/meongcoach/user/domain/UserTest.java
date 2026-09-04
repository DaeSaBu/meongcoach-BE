package com.daesabu.meongcoach.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserTest {

	@Test
	void 온보딩_회원으로_등록하면_ACTIVE_상태의_ONBOARDING_MEMBER가_생성된다() {
		User user = User.registerOnboardingMember();

		assertThat(user.getRole()).isEqualTo(UserRole.ONBOARDING_MEMBER);
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	void 게스트로_등록하면_ACTIVE_상태의_GUEST가_생성된다() {
		User user = User.registerGuest();

		assertThat(user.getRole()).isEqualTo(UserRole.GUEST);
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	void 온보딩_회원을_승격하면_MEMBER가_된다() {
		User user = User.registerOnboardingMember();

		user.promoteToMember();

		assertThat(user.getRole()).isEqualTo(UserRole.MEMBER);
	}

	@Test
	void 이미_MEMBER여도_승격은_멱등이다() {
		User user = User.registerOnboardingMember();
		user.promoteToMember();

		user.promoteToMember();

		assertThat(user.getRole()).isEqualTo(UserRole.MEMBER);
	}

	// 인가 어휘 매핑이 잘못되면(예: MEMBER에 GUEST 어휘) 인가 규칙 전체가 어긋나므로 선언부를 검증한다
	@Test
	void 모든_역할은_같은_이름의_인가_어휘로_매핑된다() {
		for (UserRole role : UserRole.values()) {
			assertThat(role.authorityRole().name()).isEqualTo(role.name());
		}
	}

	@Test
	void 탈퇴하면_상태가_WITHDRAWN으로_변경된다() {
		User user = User.registerOnboardingMember();

		user.withdraw();

		assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
	}
}
