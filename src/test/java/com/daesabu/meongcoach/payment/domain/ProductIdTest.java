package com.daesabu.meongcoach.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ProductIdTest {

	@ParameterizedTest
	@EnumSource(LifeStage.class)
	void 통합_이용권은_모든_시기를_준다(LifeStage lifeStage) {
		boolean grants = ProductId.ALL_LIFETIME.grants(lifeStage);

		assertThat(grants).isTrue();
	}

	@Test
	void 시기별_이용권은_자기_시기만_준다() {
		assertThat(ProductId.JUNIOR_LIFETIME.getLifeStages()).containsExactly(LifeStage.JUNIOR);
	}
}
