package com.daesabu.meongcoach.alert.adapter.integration;

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

class SlackAlertPropertiesTest {

	private static final String WEBHOOK_URL = "https://hooks.slack.test/services/T000/B000/token";

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

	private Set<String> violatedFields(SlackAlertProperties properties) {
		return validator.validate(properties).stream()
				.map(ConstraintViolation::getPropertyPath)
				.map(Object::toString)
				.collect(Collectors.toSet());
	}

	@Test
	void 켜져_있고_webhook_URL이_있으면_위반이_없다() {
		assertThat(violatedFields(new SlackAlertProperties(true, WEBHOOK_URL))).isEmpty();
	}

	@Test
	void 꺼져_있으면_webhook_URL이_없어도_위반이_없다() {
		assertThat(violatedFields(new SlackAlertProperties(false, null))).isEmpty();
		assertThat(violatedFields(new SlackAlertProperties(false, ""))).isEmpty();
	}

	@Test
	void 켜져_있는데_webhook_URL이_비어_있으면_위반이다() {
		assertThat(violatedFields(new SlackAlertProperties(true, " ")))
				.containsExactly("graduationWebhookUrlPresentWhenEnabled");
	}

	@Test
	void 켜져_있는데_webhook_URL이_없으면_위반이다() {
		assertThat(violatedFields(new SlackAlertProperties(true, null)))
				.containsExactly("graduationWebhookUrlPresentWhenEnabled");
	}
}
