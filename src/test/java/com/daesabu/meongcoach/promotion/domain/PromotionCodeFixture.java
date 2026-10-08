package com.daesabu.meongcoach.promotion.domain;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.springframework.test.util.ReflectionTestUtils;

public final class PromotionCodeFixture {

	private PromotionCodeFixture() {
	}

	public static PromotionCode create(String code, Set<EntitlementType> entitlementTypes, AccessTerm accessTerm,
			Instant redeemableUntil, Integer maxRedemptions) {
		PromotionCode promotionCode = new PromotionCode();
		ReflectionTestUtils.setField(promotionCode, "code", code);
		ReflectionTestUtils.setField(promotionCode, "entitlementTypes", new HashSet<>(entitlementTypes));
		ReflectionTestUtils.setField(promotionCode, "accessTerm", accessTerm);
		ReflectionTestUtils.setField(promotionCode, "redeemableUntil", redeemableUntil);
		ReflectionTestUtils.setField(promotionCode, "maxRedemptions", maxRedemptions);
		return promotionCode;
	}
}
