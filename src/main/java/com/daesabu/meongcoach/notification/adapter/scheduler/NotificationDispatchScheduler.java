package com.daesabu.meongcoach.notification.adapter.scheduler;

import com.daesabu.meongcoach.notification.application.provided.ScheduledNotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.dto.ScheduledNotificationDispatchRequest;
import com.daesabu.meongcoach.notification.domain.exception.NotificationDeliveryException;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "meongcoach.notification.scheduler.enabled", havingValue = "true")
public class NotificationDispatchScheduler {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final int MAX_ATTEMPTS = 3;
	private static final Duration RETRY_DELAY = Duration.ofMinutes(2);

	private final ScheduledNotificationDispatcher dispatcher;
	private final TaskScheduler tasks;
	private final Clock clock;

	@Scheduled(cron = "0 0 8,11,17 * * *", zone = "Asia/Seoul")
	public void dispatch() {
		dispatch(request(ZonedDateTime.now(clock)), 1);
	}

	void dispatch(ScheduledNotificationDispatchRequest request, int attempt) {
		try {
			dispatcher.dispatch(request);
		} catch (NotificationDeliveryException exception) {
			if (attempt >= MAX_ATTEMPTS) {
				throw exception;
			}
			tasks.schedule(() -> dispatch(request, attempt + 1), clock.instant().plus(RETRY_DELAY));
		}
	}

	static ScheduledNotificationDispatchRequest request(ZonedDateTime firedAt) {
		ZonedDateTime seoul = firedAt.withZoneSameInstant(SEOUL);
		return new ScheduledNotificationDispatchRequest(seoul.toLocalDate(), seoul.getHour(), seoul.getMinute());
	}
}
