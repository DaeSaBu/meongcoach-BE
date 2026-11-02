package com.daesabu.meongcoach.notification.application;

import com.daesabu.meongcoach.notification.application.provided.ScheduledNotificationDispatchResult;
import com.daesabu.meongcoach.notification.application.provided.ScheduledNotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.dto.ScheduledNotificationDispatchRequest;
import com.daesabu.meongcoach.notification.application.required.NotificationScheduleRepository;
import com.daesabu.meongcoach.notification.application.required.ScheduledNotificationDispatchRepository;
import com.daesabu.meongcoach.notification.domain.NotificationTime;
import com.daesabu.meongcoach.notification.domain.ScheduledNotificationDispatch;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class ScheduledNotificationDispatchService implements ScheduledNotificationDispatcher {
	private final ScheduledNotificationDispatchRepository dispatches;
	private final NotificationScheduleRepository schedules;
	private final NotificationDeliveryService delivery;
	private final Clock clock;

	@Override
	@Transactional
	public ScheduledNotificationDispatchResult dispatch(ScheduledNotificationDispatchRequest request) {
		ScheduledNotificationDispatch dispatch = ScheduledNotificationDispatch.of(
				request.date(), request.hour(), request.minute(), clock.instant());
		Instant scheduledAt = dispatch.getScheduledAt();

		dispatches.lock(scheduledAt.toString());
		if (dispatches.existsBySlotScheduledAt(scheduledAt)) {
			return new ScheduledNotificationDispatchResult(0, true);
		}
		NotificationTime time = dispatch.time();
		List<Long> userIds = schedules.findUserIdsByTimeEnabled(time.hour(), time.minute());
		int fids = delivery.deliver(userIds, dispatch::toPushNotification);
		dispatches.save(dispatch);
		return new ScheduledNotificationDispatchResult(fids, false);
	}
}
