package com.daesabu.meongcoach.user.adapter.client;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 잘못된 설정이 기동 시점에 걸러지는지 제약 자체를 검증한다.
 * 특히 audiences가 비면 모든 로그인이 aud 불일치로 거부되므로 반드시 막아야 한다.
 */
@DisplayName("카카오 연동 설정")
class KakaoPropertiesTest {

	private static final String VALID_ISSUER = "https://kauth.kakao.com";
	private static final String VALID_JWK_SET_URI = "https://kauth.kakao.com/.well-known/jwks.json";
	private static final List<String> VALID_AUDIENCES = List.of("test-kakao-native-app-key");

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

	private Set<String> violatedFields(KakaoProperties properties) {
		return validator.validate(properties).stream()
				.map(ConstraintViolation::getPropertyPath)
				.map(Object::toString)
				.collect(Collectors.toSet());
	}

	@Test
	@DisplayName("모든 값이 올바르면 위반이 없다")
	void validPropertiesHaveNoViolation() {
		KakaoProperties properties = new KakaoProperties(VALID_ISSUER, VALID_JWK_SET_URI, VALID_AUDIENCES);

		assertThat(violatedFields(properties)).isEmpty();
	}

	@Test
	@DisplayName("발급자가 비어 있으면 위반이다")
	void blankIssuerIsRejected() {
		KakaoProperties properties = new KakaoProperties(" ", VALID_JWK_SET_URI, VALID_AUDIENCES);

		assertThat(violatedFields(properties)).containsExactly("issuer");
	}

	@Test
	@DisplayName("공개 키 주소가 비어 있으면 위반이다")
	void blankJwkSetUriIsRejected() {
		KakaoProperties properties = new KakaoProperties(VALID_ISSUER, " ", VALID_AUDIENCES);

		assertThat(violatedFields(properties)).containsExactly("jwkSetUri");
	}

	@Test
	@DisplayName("aud 후보가 비어 있으면 위반이다")
	void emptyAudiencesIsRejected() {
		KakaoProperties properties = new KakaoProperties(VALID_ISSUER, VALID_JWK_SET_URI, List.of());

		assertThat(violatedFields(properties)).containsExactly("audiences");
	}

	@Test
	@DisplayName("aud 후보에 빈 값이 섞이면 위반이다")
	void blankAudienceElementIsRejected() {
		KakaoProperties properties = new KakaoProperties(VALID_ISSUER, VALID_JWK_SET_URI, List.of("app-key", " "));

		assertThat(violatedFields(properties)).containsExactly("audiences[1].<list element>");
	}
}
