package com.daesabu.meongcoach.notification.application;

import com.daesabu.meongcoach.notification.application.provided.ScheduledNotificationDispatchResult;
import com.daesabu.meongcoach.notification.application.provided.ScheduledNotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.dto.ScheduledNotificationDispatchRequest;
import com.daesabu.meongcoach.notification.application.required.NotificationScheduleRepository;
import com.daesabu.meongcoach.notification.application.required.ScheduledNotificationDispatchRepository;
import com.daesabu.meongcoach.notification.domain.NotificationSlot;
import com.daesabu.meongcoach.notification.domain.NotificationTime;
import com.daesabu.meongcoach.notification.domain.PushNotification;
import com.daesabu.meongcoach.notification.domain.ScheduledNotificationDispatch;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class ScheduledNotificationDispatchService implements ScheduledNotificationDispatcher {
	private static final Map<Integer, Map.Entry<String, String>> MESSAGES = Map.of(
			8, Map.entry("아침 훈련 시간이에요", "오늘의 훈련을 시작해 볼까요?"),
			11, Map.entry("점심 훈련 시간이에요", "잠깐 짬을 내서 한 가지만 연습해 볼까요?"),
			17, Map.entry("저녁 훈련 시간이에요", "오늘의 훈련을 마무리해 볼까요?")
	);
	private static final Map.Entry<String, String> DEFAULT_MESSAGE = Map.entry("훈련 시간이에요", "오늘의 훈련을 시작해 볼까요?");

	private final ScheduledNotificationDispatchRepository dispatches;
	private final NotificationScheduleRepository schedules;
	private final NotificationDeliveryService delivery;
	private final Clock clock;

	@Override
	@Transactional
	public ScheduledNotificationDispatchResult dispatch(ScheduledNotificationDispatchRequest request) {
		NotificationTime time = new NotificationTime(request.hour(), request.minute());
		NotificationSlot slot = new NotificationSlot(request.date(), time);
		slot.validateDispatchable(clock.instant());
		Instant scheduledAt = slot.scheduledAt();

		dispatches.lock(scheduledAt.toString());
		if (dispatches.existsByScheduledAt(scheduledAt)) {
			log.info("event=notification_dispatch type=SCHEDULED scheduledAt={} fids=0 duplicate=true", scheduledAt);
			return new ScheduledNotificationDispatchResult(0, true);
		}
		List<Long> userIds = schedules.findUserIdsByTimeEnabled(time.hour(), time.minute());
		int fids = delivery.deliver(userIds, fid -> message(fid, time, scheduledAt));
		dispatches.save(ScheduledNotificationDispatch.complete(scheduledAt));
		log.info("event=notification_dispatch type=SCHEDULED scheduledAt={} fids={} duplicate=false", scheduledAt, fids);
		return new ScheduledNotificationDispatchResult(fids, false);
	}

	private PushNotification message(String fid, NotificationTime time, Instant scheduledAt) {
		Map.Entry<String, String> content = MESSAGES.getOrDefault(time.hour(), DEFAULT_MESSAGE);
		Map<String, String> data = Map.of("hour", String.valueOf(time.hour()), "deepLink", "meongcoach://training-curriculum");
		return new PushNotification(fid, content.getKey(), content.getValue(), data, scheduledAt.toString());
	}
}
