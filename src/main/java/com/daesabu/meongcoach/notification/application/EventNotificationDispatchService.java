package com.daesabu.meongcoach.notification.application;

import com.daesabu.meongcoach.notification.application.provided.EventNotificationDispatchResult;
import com.daesabu.meongcoach.notification.application.provided.EventNotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.dto.EventNotificationDispatchRequest;
import com.daesabu.meongcoach.notification.application.required.NotificationEventSettingRepository;
import com.daesabu.meongcoach.notification.domain.NotificationEventContent;
import com.daesabu.meongcoach.notification.domain.NotificationEventType;
import com.daesabu.meongcoach.notification.domain.NotificationTarget;
import java.util.List;
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
public class EventNotificationDispatchService implements EventNotificationDispatcher {

	private final NotificationEventSettingRepository settings;
	private final NotificationDeliveryService delivery;

	@Override
	@Transactional
	public EventNotificationDispatchResult dispatch(EventNotificationDispatchRequest request) {
		NotificationEventType type = NotificationEventType.from(request.type());
		NotificationTarget target = NotificationTarget.of(request.target().kind(), request.target().userIds());
		NotificationEventContent content = new NotificationEventContent(request.title(), request.body(), request.deepLink());

		List<Long> userIds = findEnabledUserIds(type, target);
		int fids = delivery.deliver(userIds, fid -> content.toPushNotification(fid, type));
		log.info("event=notification_dispatch type={} target={} fids={} duplicate=false", type, request.target().kind(), fids);
		return new EventNotificationDispatchResult(fids);
	}

	private List<Long> findEnabledUserIds(NotificationEventType type, NotificationTarget target) {
		if (target.all()) {
			return settings.findUserIdsByTypeEnabled(type);
		}
		return settings.findUserIdsByTypeEnabledAndUserIdIn(type, target.userIds());
	}
}
