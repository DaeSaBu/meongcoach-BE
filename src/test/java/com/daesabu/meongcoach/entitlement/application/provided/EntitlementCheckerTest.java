package com.daesabu.meongcoach.entitlement.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.entitlement.application.required.EntitlementRepository;
import com.daesabu.meongcoach.entitlement.domain.EntitlementFixture;
import com.daesabu.meongcoach.entitlement.domain.exception.EntitlementRequiredException;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.support.ApplicationTest;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class EntitlementCheckerTest {

	private static final Long USER_ID = 42L;
	private static final Long OTHER_USER_ID = 43L;

	@Autowired
	private EntitlementChecker entitlementChecker;

	@Autowired
	private EntitlementRepository entitlementRepository;

	private void grant(Long userId, EntitlementType type, Instant expiresAt) {
		entitlementRepository.save(EntitlementFixture.create(userId, type, expiresAt));
	}

	@Test
	void 만료_시각이_없는_이용권이_있으면_true를_반환한다() {
		grant(USER_ID, EntitlementType.PUPPY, null);

		boolean hasEntitlement = entitlementChecker.hasEntitlement(USER_ID, EntitlementType.PUPPY);

		assertThat(hasEntitlement).isTrue();
	}

	@Test
	void 만료_시각이_지나지_않은_이용권이_있으면_true를_반환한다() {
		grant(USER_ID, EntitlementType.PUPPY, Instant.now().plus(Duration.ofDays(30)));

		boolean hasEntitlement = entitlementChecker.hasEntitlement(USER_ID, EntitlementType.PUPPY);

		assertThat(hasEntitlement).isTrue();
	}

	@Test
	void 만료_시각이_지난_이용권만_있으면_false를_반환한다() {
		grant(USER_ID, EntitlementType.PUPPY, Instant.now().minus(Duration.ofDays(1)));

		boolean hasEntitlement = entitlementChecker.hasEntitlement(USER_ID, EntitlementType.PUPPY);

		assertThat(hasEntitlement).isFalse();
	}

	@Test
	void 이용권이_없으면_false를_반환한다() {
		boolean hasEntitlement = entitlementChecker.hasEntitlement(USER_ID, EntitlementType.PUPPY);

		assertThat(hasEntitlement).isFalse();
	}

	@Test
	void 다른_종류의_이용권만_있으면_false를_반환한다() {
		grant(USER_ID, EntitlementType.JUNIOR, null);

		boolean hasEntitlement = entitlementChecker.hasEntitlement(USER_ID, EntitlementType.PUPPY);

		assertThat(hasEntitlement).isFalse();
	}

	@Test
	void 다른_회원의_이용권은_보유로_보지_않는다() {
		grant(OTHER_USER_ID, EntitlementType.PUPPY, null);

		boolean hasEntitlement = entitlementChecker.hasEntitlement(USER_ID, EntitlementType.PUPPY);

		assertThat(hasEntitlement).isFalse();
	}

	@Test
	void 쓸_수_있는_이용권이_있으면_검증을_통과한다() {
		grant(USER_ID, EntitlementType.PUPPY, null);

		assertThatCode(() -> entitlementChecker.validateEntitlement(USER_ID, EntitlementType.PUPPY))
				.doesNotThrowAnyException();
	}

	@Test
	void 쓸_수_있는_이용권이_없으면_EntitlementRequiredException을_던진다() {
		grant(USER_ID, EntitlementType.PUPPY, Instant.now().minus(Duration.ofDays(1)));

		assertThatThrownBy(() -> entitlementChecker.validateEntitlement(USER_ID, EntitlementType.PUPPY))
				.isInstanceOf(EntitlementRequiredException.class);
	}
}
