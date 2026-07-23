package com.daesabu.meongcoach.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.daesabu.meongcoach.user.domain.User;

@DataJpaTest
class BaseEntityTest {

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void persistInitializesCreatedAtAndUpdatedAt() {
		User user = User.registerMember();

		entityManager.persistAndFlush(user);

		assertThat(user.getCreatedAt()).isNotNull();
		assertThat(user.getUpdatedAt()).isNotNull();
	}

	@Test
	void updateRenewsUpdatedAtOnly() {
		User user = User.registerMember();
		entityManager.persistAndFlush(user);
		LocalDateTime createdAt = user.getCreatedAt();
		LocalDateTime updatedAt = user.getUpdatedAt();

		user.withdraw();
		entityManager.flush();

		assertThat(user.getCreatedAt()).isEqualTo(createdAt);
		assertThat(user.getUpdatedAt()).isAfterOrEqualTo(updatedAt);
	}
}
