package com.daesabu.meongcoach.user.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 잘못된 설정이 기동 시점에 걸러지는지 제약 자체를 검증한다.
 * 특히 audiences가 비면 모든 로그인이 aud 불일치로 거부되므로 반드시 막아야 한다.
 */
class ApplePropertiesTest {

	private static final String VALID_ISSUER = "https://appleid.apple.com";
	private static final String VALID_JWK_SET_URI = "https://appleid.apple.com/auth/keys";
	private static final List<String> VALID_AUDIENCES = List.of("com.daesabu.meongcoach");

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

	private Set<String> violatedFields(AppleProperties properties) {
		return validator.validate(properties).stream()
				.map(ConstraintViolation::getPropertyPath)
				.map(Object::toString)
				.collect(Collectors.toSet());
	}

	@Test
	void 모든_값이_올바르면_위반이_없다() {
		AppleProperties properties = new AppleProperties(VALID_ISSUER, VALID_JWK_SET_URI, VALID_AUDIENCES);

		assertThat(violatedFields(properties)).isEmpty();
	}

	@Test
	void 발급자가_비어_있으면_위반이다() {
		AppleProperties properties = new AppleProperties(" ", VALID_JWK_SET_URI, VALID_AUDIENCES);

		assertThat(violatedFields(properties)).containsExactly("issuer");
	}

	@Test
	void 공개_키_주소가_비어_있으면_위반이다() {
		AppleProperties properties = new AppleProperties(VALID_ISSUER, " ", VALID_AUDIENCES);

		assertThat(violatedFields(properties)).containsExactly("jwkSetUri");
	}

	@Test
	void aud_후보가_비어_있으면_위반이다() {
		AppleProperties properties = new AppleProperties(VALID_ISSUER, VALID_JWK_SET_URI, List.of());

		assertThat(violatedFields(properties)).containsExactly("audiences");
	}

	@Test
	void aud_후보에_빈_값이_섞이면_위반이다() {
		AppleProperties properties = new AppleProperties(VALID_ISSUER, VALID_JWK_SET_URI, List.of("bundle-id", " "));

		assertThat(violatedFields(properties)).containsExactly("audiences[1].<list element>");
	}
}
