package com.daesabu.meongcoach.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AppVersionPropertiesTest {

	private static final AppVersionProperties PROPERTIES = new AppVersionProperties(Map.of(
			AppPlatform.IOS, AppVersion.from("2.1.0"),
			AppPlatform.ANDROID, AppVersion.from("2.0.0")
	));

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

	private Set<String> violatedFields(AppVersionProperties properties) {
		return validator.validate(properties).stream()
				.map(ConstraintViolation::getPropertyPath)
				.map(Object::toString)
				.collect(Collectors.toSet());
	}

	@Test
	void 모든_플랫폼의_최소_버전이_있으면_위반이_없다() {
		assertThat(violatedFields(PROPERTIES)).isEmpty();
	}

	@Test
	void 최소_버전이_없는_플랫폼이_있으면_위반이다() {
		AppVersionProperties properties = new AppVersionProperties(Map.of(AppPlatform.IOS, AppVersion.from("2.0.0")));

		assertThat(violatedFields(properties)).containsExactly("minimumForEveryPlatform");
	}

	@Test
	void 최소_버전_설정이_없으면_위반이다() {
		AppVersionProperties properties = new AppVersionProperties(null);

		assertThat(violatedFields(properties)).containsExactly("minimumForEveryPlatform");
	}

	@Test
	void 최소_버전_이상이면_통과한다() {
		assertThatCode(() -> PROPERTIES.verify("ios", "2.1.0")).doesNotThrowAnyException();
		assertThatCode(() -> PROPERTIES.verify("ios", "2.2.0")).doesNotThrowAnyException();
	}

	@Test
	void 최소_버전보다_낮으면_업데이트가_필요하다() {
		assertThatThrownBy(() -> PROPERTIES.verify("ios", "2.0.9"))
				.isInstanceOf(AppUpdateRequiredException.class);
	}

	@Test
	void 플랫폼마다_자기_최소_버전으로_판단한다() {
		assertThatCode(() -> PROPERTIES.verify("android", "2.0.0")).doesNotThrowAnyException();
		assertThatThrownBy(() -> PROPERTIES.verify("ios", "2.0.0"))
				.isInstanceOf(AppUpdateRequiredException.class);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = " ")
	void 앱_버전이_없으면_업데이트가_필요하다(String version) {
		assertThatThrownBy(() -> PROPERTIES.verify("ios", version))
				.isInstanceOf(AppUpdateRequiredException.class);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = " ")
	void 플랫폼이_없으면_업데이트가_필요하다(String platform) {
		assertThatThrownBy(() -> PROPERTIES.verify(platform, "2.1.0"))
				.isInstanceOf(AppUpdateRequiredException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = {"web", "IOS", "windows"})
	void 지원하지_않는_플랫폼이면_형식_오류다(String platform) {
		assertThatThrownBy(() -> PROPERTIES.verify(platform, "2.1.0"))
				.isInstanceOf(InvalidAppVersionException.class);
	}

	@Test
	void 버전_형식이_틀리면_형식_오류다() {
		assertThatThrownBy(() -> PROPERTIES.verify("android", "latest"))
				.isInstanceOf(InvalidAppVersionException.class);
	}
}
