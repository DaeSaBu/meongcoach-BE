package com.daesabu.meongcoach.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(UserQueryService.class)
class UserQueryServiceTest {

	private static final Long UNREGISTERED_USER_ID = 999L;

	@Autowired
	private UserQueryService userQueryService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 활성_회원은_등록된_회원이다() {
		Long userId = userRepository.save(User.registerUser()).getId();

		assertThat(userQueryService.isRegistered(userId)).isTrue();
	}

	@Test
	void 탈퇴한_회원은_등록되지_않은_회원으로_취급한다() {
		User user = userRepository.save(User.registerUser());
		user.withdraw();
		entityManager.flush();
		entityManager.clear();

		assertThat(userQueryService.isRegistered(user.getId())).isFalse();
	}

	@Test
	void 없는_회원_ID는_등록되지_않은_회원이다() {
		assertThat(userQueryService.isRegistered(UNREGISTERED_USER_ID)).isFalse();
	}

	@Test
	void ID로_회원을_조회한다() {
		Long userId = userRepository.save(User.registerUser()).getId();

		User found = userQueryService.findById(userId);

		assertThat(found.getId()).isEqualTo(userId);
	}

	@Test
	void 없는_회원_ID로_조회하면_예외를_던진다() {
		assertThatThrownBy(() -> userQueryService.findById(UNREGISTERED_USER_ID))
				.isInstanceOf(UserNotFoundException.class);
	}
}
