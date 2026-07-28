package com.daesabu.meongcoach.shared.security;

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
@DisplayName("JWT 설정")
class JwtPropertiesTest {

	private static final String VALID_ISSUER = "meongcoach";
	private static final String VALID_SECRET = "meongcoach-test-only-jwt-secret-key-32b";
	private static final Duration VALID_ACCESS_TOKEN_VALIDITY = Duration.ofHours(1);
	private static final Duration VALID_REFRESH_TOKEN_VALIDITY = Duration.ofDays(14);

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

	// 한 필드가 여러 제약을 동시에 어길 수 있으므로 위반 필드 이름만 모은다
	private Set<String> violatedFields(JwtProperties properties) {
		return validator.validate(properties).stream()
				.map(ConstraintViolation::getPropertyPath)
				.map(Object::toString)
				.collect(Collectors.toSet());
	}

	@Test
	@DisplayName("모든 값이 올바르면 위반이 없다")
	void validPropertiesHaveNoViolation() {
		JwtProperties properties = new JwtProperties(VALID_ISSUER, VALID_SECRET,
				VALID_ACCESS_TOKEN_VALIDITY, VALID_REFRESH_TOKEN_VALIDITY);

		assertThat(violatedFields(properties)).isEmpty();
	}

	@Test
	@DisplayName("발급자가 비어 있으면 위반이다")
	void blankIssuerIsRejected() {
		JwtProperties properties = new JwtProperties(" ", VALID_SECRET,
				VALID_ACCESS_TOKEN_VALIDITY, VALID_REFRESH_TOKEN_VALIDITY);

		assertThat(violatedFields(properties)).containsExactly("issuer");
	}

	@Test
	@DisplayName("서명 키가 32자 미만이면 위반이다")
	void shortSecretIsRejected() {
		JwtProperties properties = new JwtProperties(VALID_ISSUER, "too-short-secret",
				VALID_ACCESS_TOKEN_VALIDITY, VALID_REFRESH_TOKEN_VALIDITY);

		assertThat(violatedFields(properties)).containsExactly("secret");
	}

	@Test
	@DisplayName("서명 키가 없으면 위반이다")
	void nullSecretIsRejected() {
		JwtProperties properties = new JwtProperties(VALID_ISSUER, null,
				VALID_ACCESS_TOKEN_VALIDITY, VALID_REFRESH_TOKEN_VALIDITY);

		assertThat(violatedFields(properties)).containsExactly("secret");
	}

	@Test
	@DisplayName("토큰 유효기간이 없으면 위반이다")
	void nullTokenValidityIsRejected() {
		JwtProperties properties = new JwtProperties(VALID_ISSUER, VALID_SECRET, null, null);

		assertThat(violatedFields(properties))
				.containsExactlyInAnyOrder("accessTokenValidity", "refreshTokenValidity");
	}

	@Test
	@DisplayName("서명 키로 HS256 키를 만든다")
	void secretKeyUsesHmacSha256() {
		JwtProperties properties = new JwtProperties(VALID_ISSUER, VALID_SECRET,
				VALID_ACCESS_TOKEN_VALIDITY, VALID_REFRESH_TOKEN_VALIDITY);

		assertThat(properties.secretKey().getAlgorithm()).isEqualTo("HmacSHA256");
	}
}
