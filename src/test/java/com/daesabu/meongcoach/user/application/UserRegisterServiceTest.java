package com.daesabu.meongcoach.user.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(UserRegisterService.class)
class UserRegisterServiceTest {

	@Autowired
	private UserRegisterService userRegisterService;

	@Autowired
	private UserRepository userRepository;

	@Test
	void 등록하면_온보딩_전_활성_회원이_저장되고_ID를_반환한다() {
		Long userId = userRegisterService.register();

		User user = userRepository.findById(userId).orElseThrow();
		assertThat(user.isOnboarding()).isTrue();
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}
}
