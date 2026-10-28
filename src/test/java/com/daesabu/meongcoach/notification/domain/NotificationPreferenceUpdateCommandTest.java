package com.daesabu.meongcoach.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationHourException;
import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationHoursException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NotificationPreferenceUpdateCommandTest {

	@Test
	void 허용_시각_세_개가_모두_있으면_시각별_수신_여부를_돌려준다() {
		NotificationPreferenceUpdateCommand command = new NotificationPreferenceUpdateCommand(Map.of(
				17, false,
				8, true,
				11, false
		));

		assertThat(command.enabledFor(8)).isTrue();
		assertThat(command.enabledFor(11)).isFalse();
		assertThat(command.enabledFor(17)).isFalse();
	}

	@Test
	void 시각이_빠지면_InvalidNotificationHoursException을_던진다() {
		Map<Integer, Boolean> enabledByHour = Map.of(8, true, 11, true);

		assertThatThrownBy(() -> new NotificationPreferenceUpdateCommand(enabledByHour))
				.isInstanceOf(InvalidNotificationHoursException.class);
	}

	@Test
	void 허용하지_않는_시각이_섞이면_InvalidNotificationHoursException을_던진다() {
		Map<Integer, Boolean> enabledByHour = Map.of(8, true, 11, true, 20, true);

		assertThatThrownBy(() -> new NotificationPreferenceUpdateCommand(enabledByHour))
				.isInstanceOf(InvalidNotificationHoursException.class);
	}

	@Test
	void 허용_시각_밖의_시각이_더_있으면_InvalidNotificationHoursException을_던진다() {
		Map<Integer, Boolean> enabledByHour = Map.of(8, true, 11, true, 17, true, 20, true);

		assertThatThrownBy(() -> new NotificationPreferenceUpdateCommand(enabledByHour))
				.isInstanceOf(InvalidNotificationHoursException.class);
	}

	@Test
	void 시각이_0에서_23_밖이면_InvalidNotificationHourException을_던진다() {
		Map<Integer, Boolean> enabledByHour = Map.of(8, true, 11, true, 24, true);

		assertThatThrownBy(() -> new NotificationPreferenceUpdateCommand(enabledByHour))
				.isInstanceOf(InvalidNotificationHourException.class);
	}

	@Test
	void 수신_여부가_null이면_NullPointerException을_던진다() {
		Map<Integer, Boolean> enabledByHour = new HashMap<>();
		enabledByHour.put(8, true);
		enabledByHour.put(11, null);
		enabledByHour.put(17, true);

		assertThatThrownBy(() -> new NotificationPreferenceUpdateCommand(enabledByHour))
				.isInstanceOf(NullPointerException.class);
	}
}
