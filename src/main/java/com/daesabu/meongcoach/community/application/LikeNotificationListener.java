package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.domain.LikeNotification;
import com.daesabu.meongcoach.community.domain.LikeRegisteredEvent;
import com.daesabu.meongcoach.notification.application.provided.EventNotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.dto.EventNotificationDispatchRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "meongcoach.notification.like.enabled", havingValue = "true")
public class LikeNotificationListener {

	private static final String EVENT_TYPE = "LIKE";
	private static final String TARGET_KIND = "USERS";

	private final EventNotificationDispatcher eventNotificationDispatcher;

	@Async
	@TransactionalEventListener
	public void notify(LikeRegisteredEvent event) {
		List<Long> recipientIds = event.recipientIds();
		if (recipientIds.isEmpty()) {
			return;
		}
		LikeNotification notification = LikeNotification.of(event);
		eventNotificationDispatcher.dispatch(new EventNotificationDispatchRequest(EVENT_TYPE, TARGET_KIND, recipientIds,
				notification.title(), notification.body(), notification.deepLink()));
	}
}
