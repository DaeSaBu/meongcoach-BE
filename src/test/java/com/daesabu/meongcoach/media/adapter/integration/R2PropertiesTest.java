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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 잘못된 설정이 기동 시점에 걸러지는지 제약 자체를 검증한다.
 */
@DisplayName("R2 연동 설정")
class R2PropertiesTest {

	private static final String VALID_ENDPOINT = "https://test-account.r2.cloudflarestorage.com";
	private static final String VALID_ACCESS_KEY_ID = "test-access-key";
	private static final String VALID_SECRET_ACCESS_KEY = "test-secret-key";
	private static final String VALID_BUCKET = "test-bucket";
	private static final String VALID_PUBLIC_BASE_URL = "https://images.test.meongcoach.com";
	private static final Duration VALID_VALIDITY = Duration.ofMinutes(10);

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

	private Set<String> violatedFields(R2Properties properties) {
		return validator.validate(properties).stream()
				.map(ConstraintViolation::getPropertyPath)
				.map(Object::toString)
				.collect(Collectors.toSet());
	}

	@Test
	@DisplayName("모든 값이 올바르면 위반이 없다")
	void validPropertiesHaveNoViolation() {
		R2Properties properties = new R2Properties(VALID_ENDPOINT, VALID_ACCESS_KEY_ID, VALID_SECRET_ACCESS_KEY,
				VALID_BUCKET, VALID_PUBLIC_BASE_URL, VALID_VALIDITY);

		assertThat(violatedFields(properties)).isEmpty();
	}

	@Test
	@DisplayName("엔드포인트가 비어 있으면 위반이다")
	void blankEndpointIsRejected() {
		R2Properties properties = new R2Properties(" ", VALID_ACCESS_KEY_ID, VALID_SECRET_ACCESS_KEY,
				VALID_BUCKET, VALID_PUBLIC_BASE_URL, VALID_VALIDITY);

		assertThat(violatedFields(properties)).containsExactly("endpoint");
	}

	@Test
	@DisplayName("액세스 키가 비어 있으면 위반이다")
	void blankAccessKeyIdIsRejected() {
		R2Properties properties = new R2Properties(VALID_ENDPOINT, " ", VALID_SECRET_ACCESS_KEY,
				VALID_BUCKET, VALID_PUBLIC_BASE_URL, VALID_VALIDITY);

		assertThat(violatedFields(properties)).containsExactly("accessKeyId");
	}

	@Test
	@DisplayName("시크릿 키가 비어 있으면 위반이다")
	void blankSecretAccessKeyIsRejected() {
		R2Properties properties = new R2Properties(VALID_ENDPOINT, VALID_ACCESS_KEY_ID, " ",
				VALID_BUCKET, VALID_PUBLIC_BASE_URL, VALID_VALIDITY);

		assertThat(violatedFields(properties)).containsExactly("secretAccessKey");
	}

	@Test
	@DisplayName("버킷이 비어 있으면 위반이다")
	void blankBucketIsRejected() {
		R2Properties properties = new R2Properties(VALID_ENDPOINT, VALID_ACCESS_KEY_ID, VALID_SECRET_ACCESS_KEY,
				" ", VALID_PUBLIC_BASE_URL, VALID_VALIDITY);

		assertThat(violatedFields(properties)).containsExactly("bucket");
	}

	@Test
	@DisplayName("공개 도메인이 비어 있으면 위반이다")
	void blankPublicBaseUrlIsRejected() {
		R2Properties properties = new R2Properties(VALID_ENDPOINT, VALID_ACCESS_KEY_ID, VALID_SECRET_ACCESS_KEY,
				VALID_BUCKET, " ", VALID_VALIDITY);

		assertThat(violatedFields(properties)).containsExactly("publicBaseUrl");
	}

	@Test
	@DisplayName("유효 시간이 없으면 위반이다")
	void nullValidityIsRejected() {
		R2Properties properties = new R2Properties(VALID_ENDPOINT, VALID_ACCESS_KEY_ID, VALID_SECRET_ACCESS_KEY,
				VALID_BUCKET, VALID_PUBLIC_BASE_URL, null);

		assertThat(violatedFields(properties)).containsExactly("uploadUrlValidity");
	}
}
