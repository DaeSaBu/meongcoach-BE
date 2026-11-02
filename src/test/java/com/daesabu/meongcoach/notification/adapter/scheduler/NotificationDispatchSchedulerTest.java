package com.daesabu.meongcoach.notification.adapter.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.notification.application.provided.dto.ScheduledNotificationDispatchRequest;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class NotificationDispatchSchedulerTest {

	@Test
	void 실행_시각을_서울_날짜와_시와_분으로_요청한다() {
		var firedAt = ZonedDateTime.parse("2026-11-01T23:00:00.734Z");

		var request = NotificationDispatchScheduler.request(firedAt);

		assertThat(request).isEqualTo(new ScheduledNotificationDispatchRequest(LocalDate.of(2026, 11, 2), 8, 0));
	}
}
