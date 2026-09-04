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

/**
 * 잘못된 설정이 기동 시점에 걸러지는지 제약 자체를 검증한다.
 */
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
	void 모든_값이_올바르면_위반이_없다() {
		R2Properties properties = new R2Properties(VALID_ENDPOINT, VALID_ACCESS_KEY_ID, VALID_SECRET_ACCESS_KEY,
				VALID_BUCKET, VALID_PUBLIC_BASE_URL, VALID_VALIDITY);

		assertThat(violatedFields(properties)).isEmpty();
	}

	@Test
	void 엔드포인트가_비어_있으면_위반이다() {
		R2Properties properties = new R2Properties(" ", VALID_ACCESS_KEY_ID, VALID_SECRET_ACCESS_KEY,
				VALID_BUCKET, VALID_PUBLIC_BASE_URL, VALID_VALIDITY);

		assertThat(violatedFields(properties)).containsExactly("endpoint");
	}

	@Test
	void 액세스_키가_비어_있으면_위반이다() {
		R2Properties properties = new R2Properties(VALID_ENDPOINT, " ", VALID_SECRET_ACCESS_KEY,
				VALID_BUCKET, VALID_PUBLIC_BASE_URL, VALID_VALIDITY);

		assertThat(violatedFields(properties)).containsExactly("accessKeyId");
	}

	@Test
	void 시크릿_키가_비어_있으면_위반이다() {
		R2Properties properties = new R2Properties(VALID_ENDPOINT, VALID_ACCESS_KEY_ID, " ",
				VALID_BUCKET, VALID_PUBLIC_BASE_URL, VALID_VALIDITY);

		assertThat(violatedFields(properties)).containsExactly("secretAccessKey");
	}

	@Test
	void 버킷이_비어_있으면_위반이다() {
		R2Properties properties = new R2Properties(VALID_ENDPOINT, VALID_ACCESS_KEY_ID, VALID_SECRET_ACCESS_KEY,
				" ", VALID_PUBLIC_BASE_URL, VALID_VALIDITY);

		assertThat(violatedFields(properties)).containsExactly("bucket");
	}

	@Test
	void 공개_도메인이_비어_있으면_위반이다() {
		R2Properties properties = new R2Properties(VALID_ENDPOINT, VALID_ACCESS_KEY_ID, VALID_SECRET_ACCESS_KEY,
				VALID_BUCKET, " ", VALID_VALIDITY);

		assertThat(violatedFields(properties)).containsExactly("publicBaseUrl");
	}

	@Test
	void 유효_시간이_없으면_위반이다() {
		R2Properties properties = new R2Properties(VALID_ENDPOINT, VALID_ACCESS_KEY_ID, VALID_SECRET_ACCESS_KEY,
				VALID_BUCKET, VALID_PUBLIC_BASE_URL, null);

		assertThat(violatedFields(properties)).containsExactly("uploadUrlValidity");
	}
}
