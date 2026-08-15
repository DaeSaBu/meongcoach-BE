package com.daesabu.meongcoach.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("User 도메인")
class UserTest {

	@Test
	@DisplayName("온보딩 회원으로 등록하면 ACTIVE 상태의 ONBOARDING_MEMBER가 생성된다")
	void registerOnboardingMemberCreatesActiveOnboardingMember() {
		User user = User.registerOnboardingMember();

		assertThat(user.getRole()).isEqualTo(UserRole.ONBOARDING_MEMBER);
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	@DisplayName("게스트로 등록하면 ACTIVE 상태의 GUEST가 생성된다")
	void registerGuestCreatesActiveGuest() {
		User user = User.registerGuest();

		assertThat(user.getRole()).isEqualTo(UserRole.GUEST);
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	@DisplayName("온보딩 회원을 승격하면 MEMBER가 된다")
	void promoteToMemberChangesRoleToMember() {
		User user = User.registerOnboardingMember();

		user.promoteToMember();

		assertThat(user.getRole()).isEqualTo(UserRole.MEMBER);
	}

	@Test
	@DisplayName("이미 MEMBER여도 승격은 멱등이다")
	void promoteToMemberIsIdempotent() {
		User user = User.registerOnboardingMember();
		user.promoteToMember();

		user.promoteToMember();

		assertThat(user.getRole()).isEqualTo(UserRole.MEMBER);
	}

	@Test
	@DisplayName("탈퇴하면 상태가 WITHDRAWN으로 변경된다")
	void withdrawChangesStatusToWithdrawn() {
		User user = User.registerOnboardingMember();

		user.withdraw();

		assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
	}
}
