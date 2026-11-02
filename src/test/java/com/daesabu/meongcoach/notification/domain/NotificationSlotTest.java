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

	@Test
	void 시각별_문구와_예약_시각_묶음_키로_알림을_만든다() {
		var slot = NotificationSlot.of(java.time.LocalDate.of(2026, 10, 31), new NotificationTime(17, 0));

		PushNotification notification = slot.toPushNotification("fid");

		assertThat(notification.fid()).isEqualTo("fid");
		assertThat(notification.title()).isEqualTo("저녁 훈련 시간이에요");
		assertThat(notification.body()).isEqualTo("오늘의 훈련을 마무리해 볼까요?");
		assertThat(notification.data()).isEqualTo(java.util.Map.of("hour", "17", "deepLink", "meongcoach://training-curriculum"));
		assertThat(notification.collapseKey()).isEqualTo("2026-10-31T08:00:00Z");
	}

	@Test
	void 문구가_없는_시각은_기본_문구를_쓴다() {
		var slot = NotificationSlot.of(java.time.LocalDate.of(2026, 10, 31), new NotificationTime(9, 0));

		assertThat(slot.toPushNotification("fid").title()).isEqualTo("훈련 시간이에요");
	}
}
