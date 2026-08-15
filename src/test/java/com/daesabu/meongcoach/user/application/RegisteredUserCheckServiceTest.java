package com.daesabu.meongcoach.user.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.shared.security.AuthorityRole;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(RegisteredUserCheckService.class)
@DisplayName("회원 등록 확인 서비스")
class RegisteredUserCheckServiceTest {

	private static final Long UNREGISTERED_USER_ID = 999L;

	@Autowired
	private RegisteredUserCheckService registeredUserCheckService;

	@Autowired
	private UserRepository userRepository;

	@Test
	@DisplayName("저장된 회원이면 참을 반환한다")
	void isRegisteredReturnsTrueForSavedUser() {
		Long userId = userRepository.save(User.registerOnboardingMember()).getId();

		assertThat(registeredUserCheckService.isRegistered(userId)).isTrue();
	}

	@Test
	@DisplayName("저장되지 않은 회원 ID면 거짓을 반환한다")
	void isRegisteredReturnsFalseForUnknownId() {
		assertThat(registeredUserCheckService.isRegistered(UNREGISTERED_USER_ID)).isFalse();
	}

	@Test
	@DisplayName("저장된 회원이면 인가 어휘를 반환한다")
	void findRoleReturnsRoleForSavedUser() {
		Long userId = userRepository.save(User.registerOnboardingMember()).getId();

		assertThat(registeredUserCheckService.findRole(userId)).contains(AuthorityRole.ONBOARDING_MEMBER);
	}

	@Test
	@DisplayName("저장되지 않은 회원 ID면 빈 역할을 반환한다")
	void findRoleReturnsEmptyForUnknownId() {
		assertThat(registeredUserCheckService.findRole(UNREGISTERED_USER_ID)).isEmpty();
	}
}
