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
	void 활성_회원이면_true를_반환한다() {
		Long userId = userRepository.save(User.registerUser()).getId();

		assertThat(userQueryService.isActiveUser(userId)).isTrue();
	}

	@Test
	void 탈퇴한_회원이면_false를_반환한다() {
		User user = userRepository.save(User.registerUser());
		user.withdraw();
		entityManager.flush();
		entityManager.clear();

		assertThat(userQueryService.isActiveUser(user.getId())).isFalse();
	}

	@Test
	void 없는_회원_ID면_false를_반환한다() {
		assertThat(userQueryService.isActiveUser(UNREGISTERED_USER_ID)).isFalse();
	}

	@Test
	void 온보딩_중인_회원이면_온보딩이_필요하다() {
		Long userId = userRepository.save(User.registerUser()).getId();

		assertThat(userQueryService.isOnboardingUser(userId)).isTrue();
	}

	@Test
	void 정회원이면_온보딩이_필요하지_않다() {
		User user = User.registerUser();
		user.promoteToUser();
		Long userId = userRepository.save(user).getId();

		assertThat(userQueryService.isOnboardingUser(userId)).isFalse();
	}

	@Test
	void 없는_회원_ID로_온보딩_여부를_조회하면_예외를_던진다() {
		assertThatThrownBy(() -> userQueryService.isOnboardingUser(UNREGISTERED_USER_ID))
				.isInstanceOf(UserNotFoundException.class);
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
