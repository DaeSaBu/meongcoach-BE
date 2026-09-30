package com.daesabu.meongcoach.entitlement.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class EntitlementRepositoryTest {

	private static final Long USER_ID = 42L;

	@Autowired
	private EntitlementRepository entitlementRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 회원_ID로_트랜잭션_범위의_advisory_lock을_잡는다() {
		entitlementRepository.lockByUserId(USER_ID);

		assertThat(heldAdvisoryLockCount(USER_ID)).isEqualTo(1L);
	}

	@Test
	void 잠그지_않은_회원의_advisory_lock은_없다() {
		entitlementRepository.lockByUserId(USER_ID);

		assertThat(heldAdvisoryLockCount(USER_ID + 1)).isZero();
	}

	private long heldAdvisoryLockCount(Long userId) {
		Number count = (Number) entityManager.getEntityManager()
				.createNativeQuery("""
						SELECT count(*) FROM pg_locks
						WHERE locktype = 'advisory' AND pid = pg_backend_pid() AND granted
						AND classid::bigint = 0 AND objid::bigint = :userId AND objsubid = 1
						""")
				.setParameter("userId", userId)
				.getSingleResult();
		return count.longValue();
	}
}
