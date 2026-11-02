package com.daesabu.meongcoach.notification.adapter.integration;

import com.daesabu.meongcoach.notification.application.required.NotificationSender;
import com.daesabu.meongcoach.notification.domain.PushNotification;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
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
	public NotificationSendResult send(List<PushNotification> notifications) {
		List<NotificationSendResult> results = notifications.stream().gather(Gatherers.windowFixed(BATCH_SIZE))
				.map(this::sendBatch).toList();
		return NotificationSendResult.merge(results);
	}

	private NotificationSendResult sendBatch(List<PushNotification> batch) {
		List<Message> firebaseMessages = batch.stream().map(this::toMessage).toList();
		FirebaseMessaging client = messaging.getObject();
		try {
			List<SendResponse> responses = client.sendEach(firebaseMessages).getResponses();
			return toResult(batch, responses);
		} catch (FirebaseMessagingException exception) {
			return new NotificationSendResult(0, batch.size(), List.of(), exception);
		}
	}

	private NotificationSendResult toResult(List<PushNotification> batch, List<SendResponse> responses) {
		int sentCount = 0;
		int failedCount = 0;
		List<String> invalidFids = new ArrayList<>();
		for (int index = 0; index < responses.size(); index++) {
			FirebaseMessagingException exception = responses.get(index).getException();
			if (exception == null) {
				sentCount++;
				continue;
			}
			if (isInvalid(exception.getMessagingErrorCode())) {
				invalidFids.add(batch.get(index).fid());
				continue;
			}
			failedCount++;
		}
		return new NotificationSendResult(sentCount, failedCount, invalidFids, null);
	}

	private boolean isInvalid(MessagingErrorCode code) {
		return code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.SENDER_ID_MISMATCH;
	}

	private Map<String, String> data(PushNotification notification) {
		Map<String, String> data = new HashMap<>();
		if (notification.type() != null) {
			data.put("type", notification.type().name());
		}
		if (notification.hour() != null) {
			data.put("hour", String.valueOf(notification.hour()));
		}
		if (notification.deepLink().value() != null) {
			data.put("deepLink", notification.deepLink().value());
		}
		return data;
	}

	private Message toMessage(PushNotification notification) {
		return Message.builder()
				.setFid(notification.fid())
				.setNotification(Notification.builder().setTitle(notification.title()).setBody(notification.body()).build())
				.putAllData(data(notification))
				.setAndroidConfig(AndroidConfig.builder().setTtl(3_600_000).setPriority(AndroidConfig.Priority.HIGH)
						.setNotification(AndroidNotification.builder().setTag(notification.collapseKey()).build()).build())
				.setApnsConfig(ApnsConfig.builder().setAps(Aps.builder().build())
						.putHeader("apns-priority", "10")
						.putHeader("apns-collapse-id", notification.collapseKey())
						.putHeader("apns-expiration", String.valueOf(Instant.now().getEpochSecond() + 3600)).build())
				.build();
	}
}
