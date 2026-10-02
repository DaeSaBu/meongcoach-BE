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

@DataJpaTest
@Import({UserRegisterService.class, UserQueryService.class})
class UserRegisterServiceTest {

	private static final Long UNREGISTERED_USER_ID = 999L;

	@Autowired
	private UserRegisterService userRegisterService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserProfileRepository userProfileRepository;

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

	// 스토어 심사 요건·개인정보 파기 의무: 회원 행은 참조 정합성을 위해 남기되 개인정보인 프로필은 실제로 지운다
	@Test
	void 탈퇴하면_프로필_행이_삭제된다() {
		User user = userRepository.save(User.registerUser());
		UserProfileCreateCommand command =
				new UserProfileCreateCommand("멍멍이집사", null, null, "INTJ", "FEMALE", Set.of(), Set.of());
		userProfileRepository.save(UserProfile.create(user, command));
		flushAndClear();

		userRegisterService.withdraw(user.getId());
		flushAndClear();

		assertThat(userProfileRepository.existsById(user.getId())).isFalse();
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
