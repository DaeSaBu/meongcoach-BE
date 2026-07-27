package com.daesabu.meongcoach.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.user.domain.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
@DisplayName("BaseEntity 공통 필드")
class BaseEntityTest {

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("저장하면 생성 시각과 수정 시각이 초기화된다")
	void persistInitializesCreatedAtAndUpdatedAt() {
		User user = User.registerMember();

		entityManager.persistAndFlush(user);

		assertThat(user.getCreatedAt()).isNotNull();
		assertThat(user.getUpdatedAt()).isNotNull();
	}

	@Test
	@DisplayName("수정하면 수정 시각만 갱신된다")
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

	@Test
	@DisplayName("touch를 호출하면 수정 시각이 갱신된다")
	void touchRenewsUpdatedAt() {
		User user = User.registerMember();
		entityManager.persistAndFlush(user);
		LocalDateTime updatedAt = user.getUpdatedAt();

		user.touch();

		assertThat(user.getUpdatedAt()).isAfter(updatedAt);
	}
}
