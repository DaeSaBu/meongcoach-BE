package com.daesabu.meongcoach.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationHourException;
import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationHoursException;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationPreferenceUpdateCommandTest {

	@Test
	void 허용_시각_세_개가_한_번씩_있으면_시각별_설정을_돌려준다() {
		NotificationPreferenceUpdateCommand command = new NotificationPreferenceUpdateCommand(List.of(
				new HourPreference(17, false),
				new HourPreference(8, true),
				new HourPreference(11, false)
		));

		assertThat(command.isEnabled(8)).isTrue();
		assertThat(command.isEnabled(11)).isFalse();
		assertThat(command.isEnabled(17)).isFalse();
	}

	@Test
	void 시각이_빠지면_InvalidNotificationHoursException을_던진다() {
		List<HourPreference> hours = List.of(
				new HourPreference(8, true),
				new HourPreference(11, true)
		);

		assertThatThrownBy(() -> new NotificationPreferenceUpdateCommand(hours))
				.isInstanceOf(InvalidNotificationHoursException.class);
	}

	@Test
	void 시각이_중복되면_InvalidNotificationHoursException을_던진다() {
		List<HourPreference> hours = List.of(
				new HourPreference(8, true),
				new HourPreference(8, false),
				new HourPreference(17, true)
		);

		assertThatThrownBy(() -> new NotificationPreferenceUpdateCommand(hours))
				.isInstanceOf(InvalidNotificationHoursException.class);
	}

	@Test
	void 허용하지_않는_시각이_섞이면_InvalidNotificationHoursException을_던진다() {
		List<HourPreference> hours = List.of(
				new HourPreference(8, true),
				new HourPreference(11, true),
				new HourPreference(20, true)
		);

		assertThatThrownBy(() -> new NotificationPreferenceUpdateCommand(hours))
				.isInstanceOf(InvalidNotificationHoursException.class);
	}

	@Test
	void 시각이_0에서_23_밖이면_InvalidNotificationHourException을_던진다() {
		assertThatThrownBy(() -> new HourPreference(24, true))
				.isInstanceOf(InvalidNotificationHourException.class);
	}
}
