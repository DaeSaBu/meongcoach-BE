package com.daesabu.meongcoach.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("User 도메인")
class UserTest {

	@Test
	@DisplayName("회원으로 등록하면 ACTIVE 상태의 MEMBER가 생성된다")
	void registerMemberCreatesActiveMember() {
		User user = User.registerMember();

		assertThat(user.getUserType()).isEqualTo(UserType.MEMBER);
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	@DisplayName("게스트로 등록하면 ACTIVE 상태의 GUEST가 생성된다")
	void registerGuestCreatesActiveGuest() {
		User user = User.registerGuest();

		assertThat(user.getUserType()).isEqualTo(UserType.GUEST);
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	@DisplayName("탈퇴하면 상태가 WITHDRAWN으로 변경된다")
	void withdrawChangesStatusToWithdrawn() {
		User user = User.registerMember();

		user.withdraw();

		assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
	}
}
