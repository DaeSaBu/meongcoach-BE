package com.daesabu.meongcoach.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationHourException;
import java.util.List;
import org.junit.jupiter.api.Test;
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

	@Test
	void 기본_설정을_만들면_8시와_11시는_꺼지고_17시는_켜진다() {
		List<NotificationPreference> defaults = NotificationPreference.createDefaults(USER_ID);

		assertThat(defaults)
				.extracting(NotificationPreference::getUserId, NotificationPreference::getHour,
						NotificationPreference::isEnabled)
				.containsExactly(
						tuple(USER_ID, 8, false),
						tuple(USER_ID, 11, false),
						tuple(USER_ID, 17, true)
				);
	}

	@Test
	void 수신_여부를_바꾼다() {
		NotificationPreference preference = NotificationPreference.create(USER_ID, 8, false);

		preference.change(true);

		assertThat(preference.isEnabled()).isTrue();
	}
}
