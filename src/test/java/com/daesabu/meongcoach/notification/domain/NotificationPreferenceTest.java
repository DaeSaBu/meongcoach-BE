package com.daesabu.meongcoach.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationHourException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NotificationPreferenceTest {

	private static final Long USER_ID = 1L;

	@ParameterizedTest
	@ValueSource(ints = {0, 23})
	void 시각이_0에서_23_사이면_설정을_만든다(int hour) {
		NotificationPreference preference = NotificationPreference.create(USER_ID, hour, true);

		assertThat(preference.getHour()).isEqualTo(hour);
		assertThat(preference.isEnabled()).isTrue();
	}

	@ParameterizedTest
	@ValueSource(ints = {-1, 24})
	void 시각이_0에서_23_밖이면_InvalidNotificationHourException을_던진다(int hour) {
		assertThatThrownBy(() -> NotificationPreference.create(USER_ID, hour, true))
				.isInstanceOf(InvalidNotificationHourException.class);
	}
}
