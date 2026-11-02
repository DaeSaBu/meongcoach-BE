package com.daesabu.meongcoach.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationDispatchTimeException;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ScheduledNotificationDispatchTest {

	private static final LocalDate DATE = LocalDate.of(2026, 11, 2);
	private static final Instant SCHEDULED_AT = Instant.parse("2026-11-01T23:30:00Z");

	private ScheduledNotificationDispatch dispatchAt(Instant now) {
		return ScheduledNotificationDispatch.of(DATE, 8, 30, now);
	}

	@Test
	void 서울_시각을_UTC_예약_시각으로_바꾼다() {
		assertThat(dispatchAt(SCHEDULED_AT).scheduled()).isEqualTo(SCHEDULED_AT);
	}

	@Test
	void 예약_시각을_서울_기준_시각으로_돌려준다() {
		assertThat(dispatchAt(SCHEDULED_AT).time()).isEqualTo(new NotificationTime(8, 30));
	}

	@Test
	void 현재부터_1시간_전까지는_발송할_수_있다() {
		assertThatCode(() -> dispatchAt(SCHEDULED_AT)).doesNotThrowAnyException();
		assertThatCode(() -> dispatchAt(Instant.parse("2026-11-02T00:30:00Z"))).doesNotThrowAnyException();
	}

	@Test
	void 미래_시각이면_던진다() {
		assertThatThrownBy(() -> dispatchAt(Instant.parse("2026-11-01T23:29:59Z")))
				.isInstanceOf(InvalidNotificationDispatchTimeException.class);
	}

	@Test
	void 한_시간보다_오래된_시각이면_던진다() {
		assertThatThrownBy(() -> dispatchAt(Instant.parse("2026-11-02T00:30:01Z")))
				.isInstanceOf(InvalidNotificationDispatchTimeException.class);
	}
}
