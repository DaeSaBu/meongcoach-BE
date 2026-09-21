package com.daesabu.meongcoach.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfile;
import com.daesabu.meongcoach.user.domain.UserProfileCreateCommand;
import com.daesabu.meongcoach.user.domain.UserStatus;
import com.daesabu.meongcoach.user.domain.exception.UserNotFoundException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * 회원 쪽 탈퇴 처리만 본다. 자격증명·토큰 정리까지 포함한 전체 흐름은 auth 모듈의 AccountWithdrawServiceTest가 맡는다.
 */
@DataJpaTest
@Import(UserWithdrawService.class)
class UserWithdrawServiceTest {

	private static final Long UNREGISTERED_USER_ID = 999L;

	@Autowired
	private UserWithdrawService userWithdrawService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserProfileRepository userProfileRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 탈퇴하면_회원_상태가_WITHDRAWN이_되고_행은_남는다() {
		Long userId = userRepository.save(User.registerUser()).getId();

		userWithdrawService.withdraw(userId);
		flushAndClear();

		User withdrawn = userRepository.findById(userId).orElseThrow();
		assertThat(withdrawn.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
	}

	@Test
	void 탈퇴하면_프로필_행이_삭제된다() {
		User user = userRepository.save(User.registerUser());
		UserProfileCreateCommand command =
				new UserProfileCreateCommand("멍멍이집사", null, null, "INTJ", "FEMALE", Set.of(), Set.of());
		userProfileRepository.save(UserProfile.create(user, command));
		flushAndClear();

		userWithdrawService.withdraw(user.getId());
		flushAndClear();

		assertThat(userProfileRepository.existsById(user.getId())).isFalse();
	}

	@Test
	void 없는_회원_ID로_탈퇴하면_예외를_던진다() {
		assertThatThrownBy(() -> userWithdrawService.withdraw(UNREGISTERED_USER_ID))
				.isInstanceOf(UserNotFoundException.class);
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
