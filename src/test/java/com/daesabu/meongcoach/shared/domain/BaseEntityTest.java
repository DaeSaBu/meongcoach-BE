package com.daesabu.meongcoach.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.user.domain.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class BaseEntityTest {

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 저장하면_생성_시각과_수정_시각이_초기화된다() {
		User user = User.registerOnboardingMember();

		entityManager.persistAndFlush(user);

		assertThat(user.getCreatedAt()).isNotNull();
		assertThat(user.getUpdatedAt()).isNotNull();
	}

	@Test
	void 수정하면_수정_시각만_갱신된다() {
		User user = User.registerOnboardingMember();
		entityManager.persistAndFlush(user);
		LocalDateTime createdAt = user.getCreatedAt();
		LocalDateTime updatedAt = user.getUpdatedAt();

		user.withdraw();
		entityManager.flush();

		assertThat(user.getCreatedAt()).isEqualTo(createdAt);
		assertThat(user.getUpdatedAt()).isAfterOrEqualTo(updatedAt);
	}
}
