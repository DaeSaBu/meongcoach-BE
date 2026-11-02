package com.daesabu.meongcoach.notification.adapter.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.daesabu.meongcoach.notification.application.provided.ScheduledNotificationDispatchResult;
import com.daesabu.meongcoach.notification.application.provided.ScheduledNotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.dto.ScheduledNotificationDispatchRequest;
import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationDispatchTimeException;
import com.daesabu.meongcoach.notification.domain.exception.NotificationDeliveryException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.scheduling.TaskScheduler;

class NotificationDispatchSchedulerTest {

	private static final Instant NOW = Instant.parse("2026-11-01T23:00:05Z");

	private final ScheduledNotificationDispatcher dispatcher = mock(ScheduledNotificationDispatcher.class);
	private final TaskScheduler tasks = mock(TaskScheduler.class);
	private final NotificationDispatchScheduler scheduler = new NotificationDispatchScheduler(
			dispatcher, tasks, Clock.fixed(NOW, ZoneOffset.UTC));
	private final ScheduledNotificationDispatchRequest request = new ScheduledNotificationDispatchRequest(LocalDate.of(2026, 11, 2), 8, 0);

	@Test
	void 실행_시각을_서울_날짜와_시와_분으로_요청한다() {
		var firedAt = ZonedDateTime.parse("2026-11-01T23:00:00.734Z");

		var request = NotificationDispatchScheduler.request(firedAt);

		assertThat(request).isEqualTo(new ScheduledNotificationDispatchRequest(LocalDate.of(2026, 11, 2), 8, 0));
	}

	@Test
	void 발송에_실패하면_2분_뒤_같은_요청의_재시도를_예약한다() {
		var failure = new NotificationDeliveryException(new IllegalStateException("FCM unavailable"));
		given(dispatcher.dispatch(request)).willThrow(failure).willReturn(new ScheduledNotificationDispatchResult(1, false));

		scheduler.dispatch(request, 1);

		ArgumentCaptor<Runnable> retry = ArgumentCaptor.captor();
		verify(tasks).schedule(retry.capture(), eq(NOW.plusSeconds(120)));
		retry.getValue().run();
		verify(dispatcher, times(2)).dispatch(request);
		verify(tasks, times(1)).schedule(any(Runnable.class), any(Instant.class));
	}

	@Test
	void 마지막_시도가_실패하면_던지고_더_예약하지_않는다() {
		var failure = new NotificationDeliveryException(new IllegalStateException("FCM unavailable"));
		given(dispatcher.dispatch(request)).willThrow(failure);

		assertThatThrownBy(() -> scheduler.dispatch(request, 3)).isSameAs(failure);

		verify(tasks, never()).schedule(any(Runnable.class), any(Instant.class));
	}

	@Test
	void 요청값_오류가_아닌_서버_오류도_재시도한다() {
		given(dispatcher.dispatch(request)).willThrow(new DataAccessResourceFailureException("db down"));

		scheduler.dispatch(request, 1);

		verify(tasks).schedule(any(Runnable.class), eq(NOW.plusSeconds(120)));
	}

	@Test
	void 요청값_오류는_재시도하지_않는다() {
		given(dispatcher.dispatch(request)).willThrow(new InvalidNotificationDispatchTimeException(NOW));

		assertThatThrownBy(() -> scheduler.dispatch(request, 1)).isInstanceOf(InvalidNotificationDispatchTimeException.class);

		verify(tasks, never()).schedule(any(Runnable.class), any(Instant.class));
	}
}
