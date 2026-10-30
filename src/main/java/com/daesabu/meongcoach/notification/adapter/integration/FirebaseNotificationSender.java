package com.daesabu.meongcoach.notification.adapter.integration;

import com.daesabu.meongcoach.notification.application.required.NotificationSender;
import com.daesabu.meongcoach.notification.application.required.NotificationSender.NotificationBatchResult;
import com.daesabu.meongcoach.notification.domain.NotificationMessage;
import com.daesabu.meongcoach.notification.domain.exception.NotificationDeliveryException;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
		List<String> fids = new ArrayList<>(notifications.keySet());
		List<NotificationBatchResult> results = new ArrayList<>();
		for (int start = 0; start < fids.size(); start += BATCH_SIZE) {
			int end = Math.min(start + BATCH_SIZE, fids.size());
			List<String> batchFids = fids.subList(start, end);
			List<Message> messages = batchFids.stream().map(fid -> message(fid, notifications.get(fid))).toList();
			results.add(sendBatch(batchFids, messages));
		}
		return results;
	}

	private NotificationBatchResult sendBatch(List<String> fids, List<Message> messages) {
		FirebaseMessaging client = messaging.getObject();
		try {
			List<SendResponse> responses = client.sendEach(messages).getResponses();
			return FirebaseNotificationBatchResult.success(fids, responses);
		} catch (FirebaseMessagingException exception) {
			throw new NotificationDeliveryException(exception);
		}
	}

	private Message message(String fid, NotificationMessage message) {
		return Message.builder()
				.setFid(fid)
				.setNotification(Notification.builder().setTitle(message.title()).setBody(message.body()).build())
				.putAllData(message.data())
				.setAndroidConfig(AndroidConfig.builder().setTtl(3_600_000).setPriority(AndroidConfig.Priority.HIGH).build())
				.setApnsConfig(ApnsConfig.builder().setAps(Aps.builder().build())
						.putHeader("apns-priority", "10")
						.putHeader("apns-expiration", String.valueOf(Instant.now().getEpochSecond() + 3600)).build())
				.build();
	}
}
