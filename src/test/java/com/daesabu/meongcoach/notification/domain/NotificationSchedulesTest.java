package com.daesabu.meongcoach.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationSchedulesTest {

	private static final Long USER_ID = 1L;

	@Test
	void 설정이_없으면_허용_시각의_기본_설정을_모두_만든다() {
		NotificationSchedules schedules = new NotificationSchedules(List.of());

		List<NotificationSchedule> created = schedules.createMissingDefaults(USER_ID);

		assertThat(created).extracting(NotificationSchedule::time)
				.containsExactlyElementsOf(NotificationTime.ALLOWED);
	}

	@Test
	void 이미_있는_시각은_빼고_없는_시각만_만든다() {
		NotificationTime existing = new NotificationTime(8, 0);
		NotificationSchedules schedules = new NotificationSchedules(List.of(NotificationSchedule.create(USER_ID, existing, true)));

		List<NotificationSchedule> created = schedules.createMissingDefaults(USER_ID);

		assertThat(created).extracting(NotificationSchedule::time)
				.containsExactly(new NotificationTime(11, 0), new NotificationTime(17, 0));
	}

	@Test
	void 모든_시각이_있으면_만들지_않는다() {
		NotificationSchedules schedules = new NotificationSchedules(NotificationSchedule.createDefaults(USER_ID));

		assertThat(schedules.createMissingDefaults(USER_ID)).isEmpty();
	}
}
