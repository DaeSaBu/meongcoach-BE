package com.daesabu.meongcoach.notification.adapter.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class NotificationDispatchSchedulerTest {

	@Test
	void 실행_시각을_분_단위로_잘라_서울_시각과_함께_요청한다() {
		var firedAt = ZonedDateTime.parse("2026-11-02T08:00:00.734+09:00[Asia/Seoul]");

		var request = NotificationDispatchScheduler.request(firedAt);

		assertThat(request.scheduledTime()).isEqualTo(Instant.parse("2026-11-01T23:00:00Z"));
		assertThat(request.hour()).isEqualTo(8);
	}

	@Test
	void EventBridge가_넘기는_예약_시각과_같은_키를_만든다() {
		var firedAt = ZonedDateTime.parse("2026-11-02T17:00:00.120+09:00[Asia/Seoul]");

		var request = NotificationDispatchScheduler.request(firedAt);

		assertThat(request.key()).isEqualTo("2026-11-02T08:00:00Z:17");
	}

	@Test
	void 날짜가_다르면_같은_시각이어도_키가_다르다() {
		var today = NotificationDispatchScheduler.request(ZonedDateTime.parse("2026-11-02T08:00:00+09:00[Asia/Seoul]"));
		var tomorrow = NotificationDispatchScheduler.request(ZonedDateTime.parse("2026-11-03T08:00:00+09:00[Asia/Seoul]"));

		assertThat(today.key()).isNotEqualTo(tomorrow.key());
	}
}
