package com.daesabu.meongcoach.notification.adapter.integration;

import com.daesabu.meongcoach.notification.application.required.NotificationSendOutcome;
import com.daesabu.meongcoach.notification.application.required.NotificationSendResult;
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
	public NotificationSendResult send(List<PushNotification> notifications) {
		List<List<PushNotification>> batches = notifications.stream().gather(Gatherers.windowFixed(BATCH_SIZE)).toList();
		List<NotificationSendOutcome> outcomes = new ArrayList<>();
		for (List<PushNotification> batch : batches) {
			try {
				outcomes.addAll(sendBatch(batch));
			} catch (FirebaseMessagingException exception) {
				List<PushNotification> unsent = notifications.subList(outcomes.size(), notifications.size());
				outcomes.addAll(abort(unsent, exception));
				break;
			}
		}
		return new NotificationSendResult(outcomes);
	}

	private List<NotificationSendOutcome> sendBatch(List<PushNotification> batch) throws FirebaseMessagingException {
		List<Message> firebaseMessages = batch.stream().map(this::toMessage).toList();
		FirebaseMessaging client = messaging.getObject();
		List<SendResponse> responses = client.sendEach(firebaseMessages).getResponses();
		List<NotificationSendOutcome> outcomes = new ArrayList<>();
		for (int index = 0; index < batch.size(); index++) {
			outcomes.add(toOutcome(batch.get(index), responses.get(index)));
		}
		return outcomes;
	}

	private NotificationSendOutcome toOutcome(PushNotification notification, SendResponse response) {
		String fid = notification.fid();
		FirebaseMessagingException exception = response.getException();
		if (exception == null) {
			return NotificationSendOutcome.sent(fid);
		}
		if (isInvalid(exception.getMessagingErrorCode())) {
			return NotificationSendOutcome.invalid(fid, exception);
		}
		return NotificationSendOutcome.failed(fid, exception);
	}

	private List<NotificationSendOutcome> abort(List<PushNotification> unsent, FirebaseMessagingException cause) {
		return unsent.stream()
				.map(notification -> NotificationSendOutcome.aborted(notification.fid(), cause))
				.toList();
	}

	private boolean isInvalid(MessagingErrorCode code) {
		return code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.SENDER_ID_MISMATCH;
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
