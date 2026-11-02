package com.daesabu.meongcoach.notification.adapter.integration;

import com.daesabu.meongcoach.notification.application.required.NotificationSender;
import com.daesabu.meongcoach.notification.application.required.NotificationSender.NotificationBatchResult;
import com.daesabu.meongcoach.notification.domain.PushNotification;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import java.time.Instant;
import java.util.List;
import java.util.stream.Gatherers;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FirebaseNotificationSender implements NotificationSender {
	private static final int BATCH_SIZE = 500;
	private final ObjectProvider<FirebaseMessaging> messaging;

	@Override
	public List<NotificationBatchResult> send(List<PushNotification> notifications) {
		return notifications.stream().gather(Gatherers.windowFixed(BATCH_SIZE))
				.map(this::sendBatch).toList();
	}

	private NotificationBatchResult sendBatch(List<PushNotification> batch) {
		List<Message> firebaseMessages = batch.stream().map(this::toMessage).toList();
		FirebaseMessaging client = messaging.getObject();
		try {
			List<SendResponse> responses = client.sendEach(firebaseMessages).getResponses();
			return FirebaseNotificationBatchResult.success(batch, responses);
		} catch (FirebaseMessagingException exception) {
			return FirebaseNotificationBatchResult.failure(batch, exception);
		}
	}

	private Message toMessage(PushNotification notification) {
		return Message.builder()
				.setFid(notification.fid())
				.setNotification(Notification.builder().setTitle(notification.title()).setBody(notification.body()).build())
				.putAllData(notification.data())
				.setAndroidConfig(AndroidConfig.builder().setTtl(3_600_000).setPriority(AndroidConfig.Priority.HIGH)
						.setNotification(AndroidNotification.builder().setTag(notification.collapseKey()).build()).build())
				.setApnsConfig(ApnsConfig.builder().setAps(Aps.builder().build())
						.putHeader("apns-priority", "10")
						.putHeader("apns-collapse-id", notification.collapseKey())
						.putHeader("apns-expiration", String.valueOf(Instant.now().getEpochSecond() + 3600)).build())
				.build();
	}
}
