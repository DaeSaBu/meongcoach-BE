package com.daesabu.meongcoach.promotion.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.promotion.domain.exception.PromotionCodeExhaustedException;
import com.daesabu.meongcoach.promotion.domain.exception.PromotionCodeExpiredException;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PromotionCodeTest {

	private static final Long USER_ID = 1L;
	private static final Instant NOW = Instant.parse("2026-10-09T03:00:00Z");
	private static final Instant LATER = Instant.parse("2026-11-30T15:00:00Z");
	private static final Instant EARLIER = Instant.parse("2026-10-01T00:00:00Z");
	private static final AccessTerm THREE_MONTHS = new AccessTerm("P3M", null);
	private static final Set<EntitlementType> ALL_TYPES = Set.of(EntitlementType.values());

	@Test
	void 사용하면_사용자와_혜택_종료_시각을_담은_사용_기록을_만든다() {
		PromotionCode promotionCode = PromotionCodeFixture.create("WELCOME", ALL_TYPES, THREE_MONTHS, null, null);

		PromotionCodeRedemption redemption = promotionCode.redeem(USER_ID, 0, NOW);

		assertThat(redemption.getPromotionCode()).isSameAs(promotionCode);
		assertThat(redemption.getUserId()).isEqualTo(USER_ID);
		assertThat(redemption.getAccessEndsAt()).isEqualTo(Instant.parse("2027-01-09T03:00:00Z"));
	}

	@Test
	void 고정_종료_시각_코드는_그_시각을_혜택_종료_시각으로_기록한다() {
		AccessTerm untilDecember = new AccessTerm(null, LATER);
		PromotionCode promotionCode = PromotionCodeFixture.create("WINTER", ALL_TYPES, untilDecember, null, null);

		PromotionCodeRedemption redemption = promotionCode.redeem(USER_ID, 0, NOW);

		assertThat(redemption.getAccessEndsAt()).isEqualTo(LATER);
	}

	@Test
	void 입력_기한_전이면_사용할_수_있다() {
		PromotionCode promotionCode = PromotionCodeFixture.create("WELCOME", ALL_TYPES, THREE_MONTHS, LATER, null);

		PromotionCodeRedemption redemption = promotionCode.redeem(USER_ID, 0, NOW);

		assertThat(redemption.getUserId()).isEqualTo(USER_ID);
	}

	@Test
	void 입력_기한이_지나면_사용할_수_없다() {
		PromotionCode promotionCode = PromotionCodeFixture.create("WELCOME", ALL_TYPES, THREE_MONTHS, EARLIER, null);

		assertThatThrownBy(() -> promotionCode.redeem(USER_ID, 0, NOW))
				.isInstanceOf(PromotionCodeExpiredException.class);
	}

	@Test
	void 입력_기한_시각에는_사용할_수_없다() {
		PromotionCode promotionCode = PromotionCodeFixture.create("WELCOME", ALL_TYPES, THREE_MONTHS, NOW, null);

		assertThatThrownBy(() -> promotionCode.redeem(USER_ID, 0, NOW))
				.isInstanceOf(PromotionCodeExpiredException.class);
	}

	@Test
	void 고정_종료_시각이_지난_코드는_사용할_수_없다() {
		AccessTerm endedTerm = new AccessTerm(null, EARLIER);
		PromotionCode promotionCode = PromotionCodeFixture.create("SUMMER", ALL_TYPES, endedTerm, null, null);

		assertThatThrownBy(() -> promotionCode.redeem(USER_ID, 0, NOW))
				.isInstanceOf(PromotionCodeExpiredException.class);
	}

	@Test
	void 사용_수가_상한에_도달하면_사용할_수_없다() {
		PromotionCode promotionCode = PromotionCodeFixture.create("FIRST10", ALL_TYPES, THREE_MONTHS, null, 10);

		assertThatThrownBy(() -> promotionCode.redeem(USER_ID, 10, NOW))
				.isInstanceOf(PromotionCodeExhaustedException.class);
	}

	@Test
	void 사용_수가_상한보다_적으면_사용할_수_있다() {
		PromotionCode promotionCode = PromotionCodeFixture.create("FIRST10", ALL_TYPES, THREE_MONTHS, null, 10);

		PromotionCodeRedemption redemption = promotionCode.redeem(USER_ID, 9, NOW);

		assertThat(redemption.getUserId()).isEqualTo(USER_ID);
	}

	@Test
	void 상한이_없으면_사용_수와_관계없이_사용할_수_있다() {
		PromotionCode promotionCode = PromotionCodeFixture.create("WELCOME", ALL_TYPES, THREE_MONTHS, null, null);

		PromotionCodeRedemption redemption = promotionCode.redeem(USER_ID, 100_000, NOW);

		assertThat(redemption.getUserId()).isEqualTo(USER_ID);
	}
}
