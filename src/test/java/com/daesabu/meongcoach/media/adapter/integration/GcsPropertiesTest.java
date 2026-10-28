package com.daesabu.meongcoach.media.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GcsPropertiesTest {

	private static final String VALID_BUCKET = "test-ai-video-bucket";
	private static final Duration VALID_VALIDITY = Duration.ofMinutes(15);
	private static final Duration VALID_DOWNLOAD_VALIDITY = Duration.ofHours(1);

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

	private Set<String> violatedFields(GcsProperties properties) {
		return validator.validate(properties).stream()
				.map(ConstraintViolation::getPropertyPath)
				.map(Object::toString)
				.collect(Collectors.toSet());
	}

	@Test
	void 모든_값이_올바르면_위반이_없다() {
		GcsProperties properties = new GcsProperties(VALID_BUCKET, VALID_VALIDITY, VALID_DOWNLOAD_VALIDITY);

		assertThat(violatedFields(properties)).isEmpty();
	}

	@Test
	void 버킷이_비어_있으면_위반이다() {
		GcsProperties properties = new GcsProperties(" ", VALID_VALIDITY, VALID_DOWNLOAD_VALIDITY);

		assertThat(violatedFields(properties)).containsExactly("bucket");
	}

	@Test
	void 업로드_유효_시간이_없으면_위반이다() {
		GcsProperties properties = new GcsProperties(VALID_BUCKET, null, VALID_DOWNLOAD_VALIDITY);

		assertThat(violatedFields(properties)).containsExactly("uploadUrlValidity");
	}

	@Test
	void 다운로드_유효_시간이_없으면_위반이다() {
		GcsProperties properties = new GcsProperties(VALID_BUCKET, VALID_VALIDITY, null);

		assertThat(violatedFields(properties)).containsExactly("downloadUrlValidity");
	}
}
