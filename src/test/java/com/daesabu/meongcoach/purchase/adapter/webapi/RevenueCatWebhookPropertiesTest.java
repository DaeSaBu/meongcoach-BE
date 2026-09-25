package com.daesabu.meongcoach.purchase.adapter.webapi;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 인증값이 비면 누구나 웹훅으로 구매를 등록할 수 있으므로 기동 시점에 막히는지 확인한다.
 */
class RevenueCatWebhookPropertiesTest {

	private static final String WEBHOOK_AUTHORIZATION = "revenuecat-webhook-secret";

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

	private Set<String> violatedFields(RevenueCatWebhookProperties properties) {
		return validator.validate(properties).stream()
				.map(ConstraintViolation::getPropertyPath)
				.map(Object::toString)
				.collect(Collectors.toSet());
	}

	@Test
	void 인증값이_비어_있으면_위반이다() {
		RevenueCatWebhookProperties properties = new RevenueCatWebhookProperties(" ");

		assertThat(violatedFields(properties)).containsExactly("webhookAuthorization");
	}

	@Test
	void 설정한_인증값과_같으면_인증된다() {
		RevenueCatWebhookProperties properties = new RevenueCatWebhookProperties(WEBHOOK_AUTHORIZATION);

		assertThat(properties.authorizes(WEBHOOK_AUTHORIZATION)).isTrue();
	}

	@Test
	void 설정한_인증값과_다르면_인증되지_않는다() {
		RevenueCatWebhookProperties properties = new RevenueCatWebhookProperties(WEBHOOK_AUTHORIZATION);

		assertThat(properties.authorizes("Bearer " + WEBHOOK_AUTHORIZATION)).isFalse();
	}

	@Test
	void 인증값이_없으면_인증되지_않는다() {
		RevenueCatWebhookProperties properties = new RevenueCatWebhookProperties(WEBHOOK_AUTHORIZATION);

		assertThat(properties.authorizes(null)).isFalse();
	}
}
