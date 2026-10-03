package com.daesabu.meongcoach.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AppVersionTest {

	@Test
	void 점으로_구분한_세_숫자를_버전으로_해석한다() {
		AppVersion version = AppVersion.from("2.10.3");

		assertThat(version).isEqualTo(new AppVersion(2, 10, 3));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"abc", "2.0", "2.0.0.1", "2.0.0-beta", "v2.0.0", " 2.0.0", "-1.0.0", "9999999999.0.0"})
	void 세_숫자_형식이_아니면_해석할_수_없다(String value) {
		assertThatThrownBy(() -> AppVersion.from(value))
				.isInstanceOf(InvalidAppVersionException.class);
	}

	@Test
	void 자리마다_숫자로_비교한다() {
		AppVersion minimum = AppVersion.from("2.9.0");

		assertThat(AppVersion.from("2.10.0").isOlderThan(minimum)).isFalse();
		assertThat(AppVersion.from("2.8.9").isOlderThan(minimum)).isTrue();
		assertThat(AppVersion.from("1.99.99").isOlderThan(minimum)).isTrue();
		assertThat(AppVersion.from("3.0.0").isOlderThan(minimum)).isFalse();
	}

	@Test
	void 같은_버전은_오래된_버전이_아니다() {
		AppVersion version = AppVersion.from("2.0.0");

		assertThat(version.isOlderThan(AppVersion.from("2.0.0"))).isFalse();
	}
}
