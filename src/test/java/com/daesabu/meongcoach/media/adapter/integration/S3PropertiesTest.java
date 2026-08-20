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
@DisplayName("S3 연동 설정")
class S3PropertiesTest {

	private static final String VALID_REGION = "ap-northeast-2";
	private static final String VALID_ACCESS_KEY_ID = "test-access-key";
	private static final String VALID_SECRET_ACCESS_KEY = "test-secret-key";
	private static final String VALID_BUCKET = "test-video-bucket";
	private static final String VALID_PUBLIC_BASE_URL = "https://videos.test.meongcoach.com";
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

	private Set<String> violatedFields(S3Properties properties) {
		return validator.validate(properties).stream()
				.map(ConstraintViolation::getPropertyPath)
				.map(Object::toString)
				.collect(Collectors.toSet());
	}

	@Test
	@DisplayName("모든 값이 올바르면 위반이 없다")
	void validPropertiesHaveNoViolation() {
		S3Properties properties = new S3Properties(VALID_REGION, VALID_ACCESS_KEY_ID, VALID_SECRET_ACCESS_KEY,
				VALID_BUCKET, VALID_PUBLIC_BASE_URL, VALID_VALIDITY, VALID_DOWNLOAD_VALIDITY);

		assertThat(violatedFields(properties)).isEmpty();
	}

	@Test
	@DisplayName("리전이 비어 있으면 위반이다")
	void blankRegionIsRejected() {
		S3Properties properties = new S3Properties(" ", VALID_ACCESS_KEY_ID, VALID_SECRET_ACCESS_KEY,
				VALID_BUCKET, VALID_PUBLIC_BASE_URL, VALID_VALIDITY, VALID_DOWNLOAD_VALIDITY);

		assertThat(violatedFields(properties)).containsExactly("region");
	}

	@Test
	@DisplayName("액세스 키가 비어 있으면 위반이다")
	void blankAccessKeyIdIsRejected() {
		S3Properties properties = new S3Properties(VALID_REGION, " ", VALID_SECRET_ACCESS_KEY,
				VALID_BUCKET, VALID_PUBLIC_BASE_URL, VALID_VALIDITY, VALID_DOWNLOAD_VALIDITY);

		assertThat(violatedFields(properties)).containsExactly("accessKeyId");
	}

	@Test
	@DisplayName("시크릿 키가 비어 있으면 위반이다")
	void blankSecretAccessKeyIsRejected() {
		S3Properties properties = new S3Properties(VALID_REGION, VALID_ACCESS_KEY_ID, " ",
				VALID_BUCKET, VALID_PUBLIC_BASE_URL, VALID_VALIDITY, VALID_DOWNLOAD_VALIDITY);

		assertThat(violatedFields(properties)).containsExactly("secretAccessKey");
	}

	@Test
	@DisplayName("버킷이 비어 있으면 위반이다")
	void blankBucketIsRejected() {
		S3Properties properties = new S3Properties(VALID_REGION, VALID_ACCESS_KEY_ID, VALID_SECRET_ACCESS_KEY,
				" ", VALID_PUBLIC_BASE_URL, VALID_VALIDITY, VALID_DOWNLOAD_VALIDITY);

		assertThat(violatedFields(properties)).containsExactly("bucket");
	}

	@Test
	@DisplayName("공개 도메인이 비어 있으면 위반이다")
	void blankPublicBaseUrlIsRejected() {
		S3Properties properties = new S3Properties(VALID_REGION, VALID_ACCESS_KEY_ID, VALID_SECRET_ACCESS_KEY,
				VALID_BUCKET, " ", VALID_VALIDITY, VALID_DOWNLOAD_VALIDITY);

		assertThat(violatedFields(properties)).containsExactly("publicBaseUrl");
	}

	@Test
	@DisplayName("업로드 유효 시간이 없으면 위반이다")
	void nullValidityIsRejected() {
		S3Properties properties = new S3Properties(VALID_REGION, VALID_ACCESS_KEY_ID, VALID_SECRET_ACCESS_KEY,
				VALID_BUCKET, VALID_PUBLIC_BASE_URL, null, VALID_DOWNLOAD_VALIDITY);

		assertThat(violatedFields(properties)).containsExactly("uploadUrlValidity");
	}

	@Test
	@DisplayName("다운로드 유효 시간이 없으면 위반이다")
	void nullDownloadValidityIsRejected() {
		S3Properties properties = new S3Properties(VALID_REGION, VALID_ACCESS_KEY_ID, VALID_SECRET_ACCESS_KEY,
				VALID_BUCKET, VALID_PUBLIC_BASE_URL, VALID_VALIDITY, null);

		assertThat(violatedFields(properties)).containsExactly("downloadUrlValidity");
	}
}
