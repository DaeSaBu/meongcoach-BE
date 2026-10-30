package com.daesabu.meongcoach.notification.adapter.integration;

import com.daesabu.meongcoach.notification.application.required.NotificationSender;
import com.daesabu.meongcoach.notification.domain.NotificationDeliveryResult;
import com.daesabu.meongcoach.notification.domain.NotificationMessage;
import com.daesabu.meongcoach.notification.domain.exception.NotificationDeliveryException;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FirebaseNotificationSender implements NotificationSender {
	private static final int BATCH_SIZE = 500;
	private final ObjectProvider<FirebaseMessaging> messaging;

	@Override
	public List<NotificationDeliveryResult> send(List<String> fids, NotificationMessage message) {
		FirebaseMessaging client = messaging.getObject();
		Message.Builder template = message(message);
		List<Message> messages = fids.stream().map(fid -> template.setFid(fid).build()).toList();
		return IntStream.iterate(0, start -> start < messages.size(), start -> start + BATCH_SIZE)
				.mapToObj(start -> {
					int end = Math.min(start + BATCH_SIZE, messages.size());
					return sendBatch(client, fids.subList(start, end), messages.subList(start, end));
				}).flatMap(List::stream).toList();
	}

	private List<NotificationDeliveryResult> sendBatch(FirebaseMessaging client, List<String> fids, List<Message> messages) {
		try {
			List<SendResponse> responses = client.sendEach(messages).getResponses();
			return IntStream.range(0, responses.size())
					.mapToObj(index -> response(fids.get(index), responses.get(index))).toList();
		} catch (FirebaseMessagingException exception) {
			var failure = new NotificationDeliveryException(exception);
			return fids.stream().map(fid -> new NotificationDeliveryResult(fid, false, failure)).toList();
		}
	}

	private NotificationDeliveryResult response(String fid, SendResponse response) {
		if (response.isSuccessful()) {
			return new NotificationDeliveryResult(fid, false, null);
		}
		if (response.getException().getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
			return new NotificationDeliveryResult(fid, true, null);
		}
		var failure = new NotificationDeliveryException(response.getException());
		return new NotificationDeliveryResult(fid, false, failure);
	}

	private Message.Builder message(NotificationMessage message) {
		return Message.builder()
				.setNotification(Notification.builder().setTitle(message.title()).setBody(message.body()).build())
				.putAllData(message.data())
				.setAndroidConfig(AndroidConfig.builder().setTtl(3_600_000).setPriority(AndroidConfig.Priority.HIGH).build())
				.setApnsConfig(ApnsConfig.builder().setAps(Aps.builder().build())
						.putHeader("apns-priority", "10")
						.putHeader("apns-expiration", String.valueOf(Instant.now().getEpochSecond() + 3600)).build());
	}
}
