package com.daesabu.meongcoach.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationDispatchTimeException;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class NotificationSlotTest {

	private final NotificationSlot slot = NotificationSlot.of(LocalDate.of(2026, 11, 2), new NotificationTime(8, 30));

	@Test
	void 서울_시각을_UTC_예약_시각으로_바꾼다() {
		assertThat(slot.scheduledAt()).isEqualTo(Instant.parse("2026-11-01T23:30:00Z"));
	}

	@Test
	void 현재부터_1시간_전까지는_발송할_수_있다() {
		assertThatCode(() -> slot.validateDispatchable(slot.scheduledAt())).doesNotThrowAnyException();
		assertThatCode(() -> slot.validateDispatchable(Instant.parse("2026-11-02T00:30:00Z"))).doesNotThrowAnyException();
	}

	@Test
	void 미래_시각이면_던진다() {
		assertThatThrownBy(() -> slot.validateDispatchable(Instant.parse("2026-11-01T23:29:59Z")))
				.isInstanceOf(InvalidNotificationDispatchTimeException.class);
	}

	@Test
	void 한_시간보다_오래된_시각이면_던진다() {
		assertThatThrownBy(() -> slot.validateDispatchable(Instant.parse("2026-11-02T00:30:01Z")))
				.isInstanceOf(InvalidNotificationDispatchTimeException.class);
	}
}
