package com.daesabu.meongcoach.notification.adapter.scheduler;

import com.daesabu.meongcoach.notification.application.provided.NotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationDispatchRequest;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "meongcoach.notification.scheduler.enabled", havingValue = "true")
public class NotificationDispatchScheduler {

	static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private final NotificationDispatcher dispatcher;

	@Scheduled(cron = "0 0 8,11,17 * * *", zone = "Asia/Seoul")
	public void dispatch() {
		var request = request(ZonedDateTime.now(SEOUL));
		var result = dispatcher.dispatch(request);
		log.info("event=notification_dispatch key={} fids={} duplicate={}", request.key(), result.fids(), result.duplicate());
	}

	static NotificationDispatchRequest request(ZonedDateTime firedAt) {
		ZonedDateTime slot = firedAt.withZoneSameInstant(SEOUL).truncatedTo(ChronoUnit.MINUTES);
		return new NotificationDispatchRequest(slot.toInstant(), slot.getHour());
	}
}
