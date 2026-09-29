package com.daesabu.meongcoach.entitlement.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 이용권 종류의 RevenueCat ID가 빠지면 그 이용권은 동기화할 때마다 회수되므로, 첫 동기화가 아니라 기동 시점에 막히는지 확인한다.
 */
class RevenueCatPropertiesTest {

	private static ValidatorFactory validatorFactory;
	private static Validator validator;

	@BeforeAll
	static void openValidator() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@AfterAll
	static void closeValidator() {
		validatorFactory.close();
	}

	private static Map<EntitlementType, String> allEntitlementIds() {
		Map<EntitlementType, String> entitlementIds = new EnumMap<>(EntitlementType.class);
		entitlementIds.put(EntitlementType.PUPPY, "entl_puppy");
		entitlementIds.put(EntitlementType.JUNIOR, "entl_junior");
		entitlementIds.put(EntitlementType.ADULT, "entl_adult");
		entitlementIds.put(EntitlementType.SENIOR, "entl_senior");
		return entitlementIds;
	}

	private static RevenueCatProperties properties(Map<EntitlementType, String> entitlementIds) {
		return new RevenueCatProperties("https://api.revenuecat.test/v2", "api-key", "proj_test", entitlementIds);
	}

	private Set<String> violatedFields(RevenueCatProperties properties) {
		return validator.validate(properties).stream()
				.map(ConstraintViolation::getPropertyPath)
				.map(Object::toString)
				.collect(Collectors.toSet());
	}

	@Test
	void 모든_이용권_종류의_ID가_있으면_위반이_없다() {
		assertThat(violatedFields(properties(allEntitlementIds()))).isEmpty();
	}

	@Test
	void ID가_없는_이용권_종류가_있으면_위반이다() {
		Map<EntitlementType, String> entitlementIds = allEntitlementIds();
		entitlementIds.remove(EntitlementType.SENIOR);

		assertThat(violatedFields(properties(entitlementIds))).containsExactly("entitlementIdsComplete");
	}

	@Test
	void 비어_있는_ID가_있으면_위반이다() {
		Map<EntitlementType, String> entitlementIds = allEntitlementIds();
		entitlementIds.put(EntitlementType.SENIOR, " ");

		assertThat(violatedFields(properties(entitlementIds))).containsExactly("entitlementIdsComplete");
	}

	@Test
	void 설정한_ID로_이용권_종류를_찾는다() {
		RevenueCatProperties properties = properties(allEntitlementIds());

		assertThat(properties.findEntitlementType("entl_adult")).contains(EntitlementType.ADULT);
		assertThat(properties.findEntitlementType("entl_unknown")).isEmpty();
	}
}
