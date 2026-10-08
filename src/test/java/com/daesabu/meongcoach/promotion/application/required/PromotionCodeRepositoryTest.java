package com.daesabu.meongcoach.promotion.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.promotion.domain.AccessTerm;
import com.daesabu.meongcoach.promotion.domain.PromotionCode;
import com.daesabu.meongcoach.promotion.domain.PromotionCodeFixture;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class PromotionCodeRepositoryTest {

	@Autowired
	private PromotionCodeRepository promotionCodeRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 코드_문자열로_부여할_이용권과_혜택_기간을_함께_조회한다() {
		AccessTerm threeMonths = new AccessTerm("P3M", null);
		Set<EntitlementType> allTypes = Set.of(EntitlementType.values());
		entityManager.persist(PromotionCodeFixture.create("WELCOME", allTypes, threeMonths, null, null));
		entityManager.flush();
		entityManager.clear();

		Optional<PromotionCode> found = promotionCodeRepository.findByCodeForUpdate("WELCOME");

		assertThat(found).hasValueSatisfying(promotionCode -> {
			assertThat(promotionCode.getEntitlementTypes()).containsExactlyInAnyOrder(EntitlementType.values());
			assertThat(promotionCode.getAccessTerm()).isEqualTo(threeMonths);
		});
	}

	@Test
	void 없는_코드는_조회되지_않는다() {
		Optional<PromotionCode> found = promotionCodeRepository.findByCodeForUpdate("NOT_EXISTS");

		assertThat(found).isEmpty();
	}
}
