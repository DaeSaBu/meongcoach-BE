package com.daesabu.meongcoach.notification.adapter.scheduler;

import com.daesabu.meongcoach.notification.application.provided.ScheduledNotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.dto.ScheduledNotificationDispatchRequest;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "meongcoach.notification.scheduler.enabled", havingValue = "true")
public class NotificationDispatchScheduler {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private final ScheduledNotificationDispatcher dispatcher;

	@Scheduled(cron = "0 0 8,11,17 * * *", zone = "Asia/Seoul")
	public void dispatch() {
		dispatcher.dispatch(request(ZonedDateTime.now(SEOUL)));
	}

	static ScheduledNotificationDispatchRequest request(ZonedDateTime firedAt) {
		ZonedDateTime seoul = firedAt.withZoneSameInstant(SEOUL);
		return new ScheduledNotificationDispatchRequest(seoul.toLocalDate(), seoul.getHour(), seoul.getMinute());
	}
}
