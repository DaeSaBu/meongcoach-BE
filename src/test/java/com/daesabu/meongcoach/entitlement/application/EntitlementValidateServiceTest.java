package com.daesabu.meongcoach.entitlement.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.entitlement.application.required.EntitlementRepository;
import com.daesabu.meongcoach.entitlement.domain.exception.EntitlementNotOwnedException;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class EntitlementValidateServiceTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntitlementRepository entitlementRepository;

	private EntitlementModifyService modifyService;

	private EntitlementValidateService service;

	private Long userId;

	@BeforeEach
	void setUp() {
		modifyService = new EntitlementModifyService(entitlementRepository);
		service = new EntitlementValidateService(entitlementRepository);
		userId = userRepository.save(User.registerUser()).getId();
	}

	@Test
	void 활성_이용권이_있으면_통과한다() {
		modifyService.synchronize(userId, Set.of(EntitlementType.PUPPY));

		assertThatCode(() -> service.validate(userId, EntitlementType.PUPPY)).doesNotThrowAnyException();
	}

	@Test
	void 다른_종류의_이용권만_있으면_예외가_발생한다() {
		modifyService.synchronize(userId, Set.of(EntitlementType.PUPPY));

		assertThatThrownBy(() -> service.validate(userId, EntitlementType.SENIOR))
				.isInstanceOf(EntitlementNotOwnedException.class);
	}

	@Test
	void 회수된_이용권이면_예외가_발생한다() {
		modifyService.synchronize(userId, Set.of(EntitlementType.PUPPY));
		modifyService.synchronize(userId, Set.of());

		assertThatThrownBy(() -> service.validate(userId, EntitlementType.PUPPY))
				.isInstanceOf(EntitlementNotOwnedException.class);
	}

	@Test
	void 다른_회원의_이용권으로는_통과하지_않는다() {
		Long otherUserId = userRepository.save(User.registerUser()).getId();
		modifyService.synchronize(otherUserId, Set.of(EntitlementType.PUPPY));

		assertThatThrownBy(() -> service.validate(userId, EntitlementType.PUPPY))
				.isInstanceOf(EntitlementNotOwnedException.class);
	}
}
