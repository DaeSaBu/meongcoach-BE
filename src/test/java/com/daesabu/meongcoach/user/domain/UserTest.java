package com.daesabu.meongcoach.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserTest {

	@Test
	void registerMemberCreatesActiveMember() {
		User user = User.registerMember();

		assertThat(user.getUserType()).isEqualTo(UserType.MEMBER);
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	void registerGuestCreatesActiveGuest() {
		User user = User.registerGuest();

		assertThat(user.getUserType()).isEqualTo(UserType.GUEST);
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	void withdrawChangesStatusToWithdrawn() {
		User user = User.registerMember();

		user.withdraw();

		assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
	}
}
