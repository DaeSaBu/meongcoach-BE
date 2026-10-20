package com.daesabu.meongcoach.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

@DataJpaTest
@Import({UserRegisterService.class, UserQueryService.class})
class UserRegisterServiceTest {

	private static final Long UNREGISTERED_USER_ID = 999L;

	@Autowired
	private UserRegisterService userRegisterService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 등록하면_온보딩_전_활성_회원이_저장되고_ID를_반환한다() {
		Long userId = userRegisterService.register();

		User user = userRepository.findById(userId).orElseThrow();
		assertThat(user.isOnboarding()).isTrue();
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	void 탈퇴하면_회원_상태가_WITHDRAWN이_되고_행은_남는다() {
		Long userId = userRepository.save(User.registerUser()).getId();

		userRegisterService.withdraw(userId);
		flushAndClear();

		User withdrawn = userRepository.findById(userId).orElseThrow();
		assertThat(withdrawn.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
	}

	@Test
	void 탈퇴하면_프로필_행이_삭제된다() {
		User user = User.registerUser();
		user.completeOnboarding(new UserProfileCreateCommand("멍멍이집사", null, "INTJ", Set.of(), Set.of()));
		userRepository.save(user);
		Long profileId = user.getProfile().getId();
		flushAndClear();

		userRegisterService.withdraw(user.getId());
		flushAndClear();

		assertThat(entityManager.find(UserProfile.class, profileId)).isNull();
		assertThat(userRepository.findById(user.getId()).orElseThrow().getProfile()).isNull();
	}

	@Test
	void 온보딩_미완료_회원은_프로필이_없어도_탈퇴할_수_있다() {
		Long userId = userRepository.save(User.registerUser()).getId();

		userRegisterService.withdraw(userId);
		flushAndClear();

		assertThat(userRepository.findById(userId).orElseThrow().getStatus()).isEqualTo(UserStatus.WITHDRAWN);
	}

	@Test
	void 없는_회원_ID로_탈퇴하면_예외를_던진다() {
		assertThatThrownBy(() -> userRegisterService.withdraw(UNREGISTERED_USER_ID))
				.isInstanceOf(UserNotFoundException.class);
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
