package com.daesabu.meongcoach.notification.adapter.integration;

import com.daesabu.meongcoach.notification.application.required.NotificationSender;
import com.daesabu.meongcoach.notification.application.required.NotificationSender.NotificationBatchResult;
import com.daesabu.meongcoach.notification.domain.NotificationMessage;
import com.daesabu.meongcoach.notification.domain.exception.NotificationDeliveryException;
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
import java.util.Map;
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
	public List<NotificationBatchResult> send(Map<String, NotificationMessage> notifications) {
		return notifications.entrySet().stream().gather(Gatherers.windowFixed(BATCH_SIZE))
				.map(this::sendBatch).toList();
	}

	private NotificationBatchResult sendBatch(List<Map.Entry<String, NotificationMessage>> batch) {
		List<String> fids = batch.stream().map(Map.Entry::getKey).toList();
		List<Message> messages = batch.stream().map(this::toMessage).toList();
		FirebaseMessaging client = messaging.getObject();
		try {
			List<SendResponse> responses = client.sendEach(messages).getResponses();
			return FirebaseNotificationBatchResult.success(fids, responses);
		} catch (FirebaseMessagingException exception) {
			throw new NotificationDeliveryException(exception);
		}
	}

	private Message toMessage(Map.Entry<String, NotificationMessage> entry) {
		NotificationMessage message = entry.getValue();
		return Message.builder()
				.setFid(entry.getKey())
				.setNotification(Notification.builder().setTitle(message.title()).setBody(message.body()).build())
				.putAllData(message.data())
				.setAndroidConfig(AndroidConfig.builder().setTtl(3_600_000).setPriority(AndroidConfig.Priority.HIGH)
						.setNotification(AndroidNotification.builder().setTag(message.collapseKey()).build()).build())
				.setApnsConfig(ApnsConfig.builder().setAps(Aps.builder().build())
						.putHeader("apns-priority", "10")
						.putHeader("apns-collapse-id", message.collapseKey())
						.putHeader("apns-expiration", String.valueOf(Instant.now().getEpochSecond() + 3600)).build())
				.build();
	}
}
