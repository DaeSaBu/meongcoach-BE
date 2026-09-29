package com.daesabu.meongcoach.entitlement.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.entitlement.domain.exception.UnsupportedEntitlementTypeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class EntitlementTypeTest {

	@Test
	void RevenueCat_식별자를_대소문자_구분_없이_이용권_종류로_바꾼다() {
		assertThat(EntitlementType.from("puppy")).isEqualTo(EntitlementType.PUPPY);
		assertThat(EntitlementType.from("SENIOR")).isEqualTo(EntitlementType.SENIOR);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {" ", "premium"})
	void 모르는_식별자면_예외를_던진다(String value) {
		assertThatThrownBy(() -> EntitlementType.from(value))
				.isInstanceOf(UnsupportedEntitlementTypeException.class);
	}
}
