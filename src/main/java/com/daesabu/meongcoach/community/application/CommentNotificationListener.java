package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.domain.CommentNotification;
import com.daesabu.meongcoach.community.domain.CommentRegisteredEvent;
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
@ConditionalOnProperty(name = "meongcoach.notification.comment.enabled", havingValue = "true")
public class CommentNotificationListener {

	private static final String EVENT_TYPE = "COMMENT";
	private static final String TARGET_KIND = "USERS";

	private final EventNotificationDispatcher eventNotificationDispatcher;

	@Async
	@TransactionalEventListener
	public void notify(CommentRegisteredEvent event) {
		List<Long> recipientIds = event.recipientIds();
		if (recipientIds.isEmpty()) {
			return;
		}
		CommentNotification notification = CommentNotification.of(event);
		eventNotificationDispatcher.dispatch(new EventNotificationDispatchRequest(EVENT_TYPE, TARGET_KIND, recipientIds,
				notification.title(), notification.body(), notification.deepLink()));
	}
}
