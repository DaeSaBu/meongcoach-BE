package com.daesabu.meongcoach.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.user.application.provided.LocalAccountRegisterInfo;
import com.daesabu.meongcoach.user.application.required.LocalAccountRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.LocalAccount;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserRole;
import com.daesabu.meongcoach.user.domain.UserStatus;
import com.daesabu.meongcoach.user.domain.exception.DuplicateEmailException;
import com.daesabu.meongcoach.user.domain.exception.InvalidEmailException;
import com.daesabu.meongcoach.user.domain.vo.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@DataJpaTest
class LocalAccountRegisterServiceTest {

	private static final String EMAIL = "lt-0001@meongcoach.test";
	private static final String PASSWORD = "loadtest-password";

	// 저장 로직은 해시 강도와 무관하므로 테스트에서는 최소 강도로 해싱 비용을 줄인다
	private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(4);

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private LocalAccountRepository localAccountRepository;

	@Autowired
	private TestEntityManager entityManager;

	private LocalAccountRegisterService service;

	@BeforeEach
	void setUp() {
		service = new LocalAccountRegisterService(userRepository, localAccountRepository, PASSWORD_ENCODER);
	}

	@Test
	void 온보딩_전_회원과_로컬_계정을_같은_트랜잭션에서_함께_생성한다() {
		Long userId = service.register(new LocalAccountRegisterInfo(EMAIL, PASSWORD));
		entityManager.flush();
		entityManager.clear();

		User user = userRepository.findById(userId).orElseThrow();
		LocalAccount account = localAccountRepository.findByEmail(new Email(EMAIL)).orElseThrow();
		assertThat(user.getRole()).isEqualTo(UserRole.ONBOARDING_MEMBER);
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(account.getUser().getId()).isEqualTo(userId);
	}

	@Test
	void 비밀번호는_평문이_아닌_해시로_저장한다() {
		service.register(new LocalAccountRegisterInfo(EMAIL, PASSWORD));
		entityManager.flush();
		entityManager.clear();

		LocalAccount account = localAccountRepository.findByEmail(new Email(EMAIL)).orElseThrow();
		assertThat(account.getPasswordHash()).isNotEqualTo(PASSWORD);
		assertThat(PASSWORD_ENCODER.matches(PASSWORD, account.getPasswordHash())).isTrue();
	}

	@Test
	void 이미_등록된_이메일이면_DuplicateEmailException을_던진다() {
		service.register(new LocalAccountRegisterInfo(EMAIL, PASSWORD));
		entityManager.flush();

		assertThatThrownBy(() -> service.register(new LocalAccountRegisterInfo(EMAIL, "another-password")))
				.isInstanceOf(DuplicateEmailException.class);
	}

	@Test
	void 이메일_형식이_올바르지_않으면_InvalidEmailException을_던진다() {
		assertThatThrownBy(() -> service.register(new LocalAccountRegisterInfo("not-an-email", PASSWORD)))
				.isInstanceOf(InvalidEmailException.class);
	}
}
