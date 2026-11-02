package com.daesabu.meongcoach.notification.application;

import com.daesabu.meongcoach.notification.application.provided.EventNotificationDispatchResult;
import com.daesabu.meongcoach.notification.application.provided.EventNotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.ScheduledNotificationDispatchResult;
import com.daesabu.meongcoach.notification.application.provided.ScheduledNotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.dto.EventNotificationDispatchRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.ScheduledNotificationDispatchRequest;
import com.daesabu.meongcoach.notification.application.required.DeviceInstallationRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationEventSettingRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationScheduleRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationSender;
import com.daesabu.meongcoach.notification.application.required.ScheduledNotificationDispatchRepository;
import com.daesabu.meongcoach.notification.domain.DeviceInstallation;
import com.daesabu.meongcoach.notification.domain.NotificationEventContent;
import com.daesabu.meongcoach.notification.domain.NotificationEventType;
import com.daesabu.meongcoach.notification.domain.NotificationTarget;
import com.daesabu.meongcoach.notification.domain.NotificationTime;
import com.daesabu.meongcoach.notification.domain.PushNotification;
import com.daesabu.meongcoach.notification.domain.ScheduledNotificationDispatch;
import com.daesabu.meongcoach.user.application.provided.UserFinder;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class NotificationDispatchService implements ScheduledNotificationDispatcher, EventNotificationDispatcher {

	private final ScheduledNotificationDispatchRepository dispatches;
	private final NotificationScheduleRepository schedules;
	private final NotificationEventSettingRepository settings;
	private final DeviceInstallationRepository installations;
	private final UserFinder users;
	private final NotificationSender sender;
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
		List<String> fids = findFids(schedules.findUserIdsByTimeEnabled(time.hour(), time.minute()));
		send(dispatch.toPushNotifications(fids));
		dispatches.save(dispatch);
		return new ScheduledNotificationDispatchResult(fids.size(), false);
	}

	@Override
	@Transactional
	public EventNotificationDispatchResult dispatch(EventNotificationDispatchRequest request) {
		NotificationEventType type = NotificationEventType.from(request.type());
		NotificationTarget target = NotificationTarget.of(request.target().kind(), request.target().userIds());
		NotificationEventContent content = new NotificationEventContent(request.title(), request.body(), request.deepLink());

		List<String> fids = findFids(findEnabledUserIds(type, target));
		send(content.toPushNotifications(fids, type));
		return new EventNotificationDispatchResult(fids.size());
	}

	private List<Long> findEnabledUserIds(NotificationEventType type, NotificationTarget target) {
		if (target.all()) {
			return settings.findUserIdsByTypeEnabled(type);
		}
		return settings.findUserIdsByTypeEnabledAndUserIdIn(type, target.userIds());
	}

	private List<String> findFids(List<Long> userIds) {
		Set<Long> activeUserIds = users.findActiveUserIds(userIds);
		if (activeUserIds.isEmpty()) {
			return List.of();
		}
		return installations.findAllByUserIdInOrderByUserIdAscIdAsc(activeUserIds).stream()
				.map(DeviceInstallation::getFid).distinct().toList();
	}

	private void send(List<PushNotification> notifications) {
		if (notifications.isEmpty()) {
			return;
		}
		List<String> invalidFids = sender.send(notifications).stream()
				.flatMap(result -> result.invalidFids().stream()).toList();
		if (!invalidFids.isEmpty()) {
			installations.deleteAllByFidIn(invalidFids);
		}
	}
}
