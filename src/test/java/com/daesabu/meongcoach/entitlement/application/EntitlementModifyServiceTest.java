package com.daesabu.meongcoach.entitlement.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.entitlement.application.required.EntitlementRepository;
import com.daesabu.meongcoach.entitlement.domain.Entitlement;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class EntitlementModifyServiceTest {

	private static final Instant NOW = Instant.parse("2026-09-29T12:00:00Z");

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntitlementRepository entitlementRepository;

	private EntitlementModifyService service;

	private Long userId;

	@BeforeEach
	void setUp() {
		service = new EntitlementModifyService(entitlementRepository);
		userId = userRepository.save(User.registerUser()).getId();
	}

	@Test
	void 활성_종류마다_이용권을_한_행씩_저장한다() {
		service.synchronize(userId, EnumSet.allOf(EntitlementType.class), NOW);

		assertThat(entitlementRepository.findAllByUserId(userId))
				.extracting(Entitlement::getType)
				.containsExactlyInAnyOrder(EntitlementType.values());
	}

	@Test
	void 같은_활성_종류로_다시_맞춰도_행이_늘지_않는다() {
		service.synchronize(userId, Set.of(EntitlementType.PUPPY), NOW);

		service.synchronize(userId, Set.of(EntitlementType.PUPPY), NOW);

		assertThat(entitlementRepository.findAllByUserId(userId)).hasSize(1);
	}

	@Test
	void 활성_종류에서_빠진_이용권은_행을_남긴_채_회수한다() {
		service.synchronize(userId, Set.of(EntitlementType.PUPPY, EntitlementType.JUNIOR), NOW);

		service.synchronize(userId, Set.of(EntitlementType.JUNIOR), NOW);

		assertThat(entitlementRepository.findAllByUserId(userId))
				.filteredOn(entitlement -> entitlement.getType() == EntitlementType.PUPPY)
				.singleElement()
				.satisfies(puppy -> assertThat(puppy.getRevokedAt()).isEqualTo(NOW));
	}

	@Test
	void 다른_회원의_이용권은_건드리지_않는다() {
		Long otherUserId = userRepository.save(User.registerUser()).getId();
		service.synchronize(otherUserId, Set.of(EntitlementType.PUPPY), NOW);

		service.synchronize(userId, Set.of(), NOW);

		assertThat(entitlementRepository.findAllByUserId(otherUserId)).allMatch(Entitlement::isActive);
	}
}
