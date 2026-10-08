package com.daesabu.meongcoach.promotion.application.required;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.promotion.domain.AccessTerm;
import com.daesabu.meongcoach.promotion.domain.PromotionCode;
import com.daesabu.meongcoach.promotion.domain.PromotionCodeFixture;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class PromotionCodeRedemptionRepositoryTest {

	private static final Instant NOW = Instant.parse("2026-10-09T03:00:00Z");

	@Autowired
	private PromotionCodeRedemptionRepository promotionCodeRedemptionRepository;

	@Autowired
	private TestEntityManager entityManager;

	private PromotionCode promotionCode;

	@BeforeEach
	void setUp() {
		promotionCode = entityManager.persist(PromotionCodeFixture.create(
				"WELCOME", Set.of(EntitlementType.PUPPY), new AccessTerm("P3M", null), null, null));
	}

	@Test
	void 코드별_사용_수를_센다() {
		promotionCodeRedemptionRepository.save(promotionCode.redeem(1L, 0, NOW));
		promotionCodeRedemptionRepository.save(promotionCode.redeem(2L, 1, NOW));

		long count = promotionCodeRedemptionRepository.countByPromotionCode(promotionCode);

		assertThat(count).isEqualTo(2);
	}

	@Test
	void 같은_사용자는_같은_코드를_두_번_사용할_수_없다() {
		promotionCodeRedemptionRepository.saveAndFlush(promotionCode.redeem(1L, 0, NOW));

		assertThatThrownBy(() -> promotionCodeRedemptionRepository.saveAndFlush(promotionCode.redeem(1L, 1, NOW)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
