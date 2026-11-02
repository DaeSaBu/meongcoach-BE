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
import com.daesabu.meongcoach.notification.application.required.NotificationSender.NotificationSendResult;
import com.daesabu.meongcoach.notification.application.required.ScheduledNotificationDispatchRepository;
import com.daesabu.meongcoach.notification.domain.DeviceInstallation;
import com.daesabu.meongcoach.notification.domain.NotificationDeepLink;
import com.daesabu.meongcoach.notification.domain.NotificationEventType;
import com.daesabu.meongcoach.notification.domain.NotificationTarget;
import com.daesabu.meongcoach.notification.domain.PushNotification;
import com.daesabu.meongcoach.notification.domain.PushNotificationFactory;
import com.daesabu.meongcoach.notification.domain.ScheduledNotificationDispatch;
import com.daesabu.meongcoach.user.application.provided.UserFinder;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
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
		ScheduledNotificationDispatch dispatch = ScheduledNotificationDispatch.of(request.toCommand(), clock.instant());
		Instant scheduledAt = dispatch.scheduled();

		dispatches.lock(scheduledAt);
		if (dispatches.existsByScheduledAt(scheduledAt)) {
			log.info("event=notification_dispatch type=SCHEDULED scheduledAt={} duplicate=true", scheduledAt);
			return new ScheduledNotificationDispatchResult(0, true);
		}
		List<String> fids = findFids(schedules.findUserIdsByTimeEnabled(dispatch.time()));
		List<PushNotification> notifications = PushNotificationFactory.scheduled(fids, dispatch);
		deliver("type=SCHEDULED scheduledAt=" + scheduledAt, notifications);
		dispatches.save(dispatch);
		int count = fids.size();
		return new ScheduledNotificationDispatchResult(count, false);
	}

	@Override
	@Transactional
	public EventNotificationDispatchResult dispatch(EventNotificationDispatchRequest request) {
		NotificationEventType type = NotificationEventType.from(request.type());
		NotificationTarget target = NotificationTarget.of(request.targetKind(), request.userIds());
		NotificationDeepLink deepLink = new NotificationDeepLink(request.deepLink());

		List<String> fids = findFids(findEnabledUserIds(type, target));
		List<PushNotification> notifications = PushNotificationFactory.event(fids, type, request.title(), request.body(), deepLink);
		deliver("type=" + type + " target=" + request.targetKind(), notifications);
		int count = fids.size();
		return new EventNotificationDispatchResult(count);
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

	private void deliver(String label, List<PushNotification> notifications) {
		if (notifications.isEmpty()) {
			log.info("event=notification_dispatch {} fids=0", label);
			return;
		}
		NotificationSendResult result = sender.send(notifications);
		deleteInvalidInstallations(result.invalidFids());
		log.info("event=notification_dispatch {} fids={} sent={} failed={} invalid={}",
				label, notifications.size(), result.sentCount(), result.failedCount(), result.invalidFids().size());
		result.requireDelivered();
	}

	private void deleteInvalidInstallations(List<String> invalidFids) {
		if (invalidFids.isEmpty()) {
			return;
		}
		installations.deleteAllByFidIn(invalidFids);
	}
}
