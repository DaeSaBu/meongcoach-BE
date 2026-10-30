package com.daesabu.meongcoach.notification.adapter.integration;

import com.daesabu.meongcoach.notification.application.required.NotificationSendResult;
import com.daesabu.meongcoach.notification.application.required.NotificationSender;
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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Gatherers;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FirebaseNotificationSender implements NotificationSender {
	private static final int BATCH_SIZE = 500;
	private static final Map<Integer, List<String>> TEXTS = Map.of(
			8, List.of("아침 훈련 시간이에요", "오늘의 훈련을 시작해 볼까요?"),
			11, List.of("점심 훈련 시간이에요", "잠깐 짬을 내서 한 가지만 연습해 볼까요?"),
			17, List.of("저녁 훈련 시간이에요", "오늘의 훈련을 마무리해 볼까요?")
	);
	private final ObjectProvider<FirebaseMessaging> messaging;

	@Override
	public NotificationSendResult send(List<String> fids, int hour) {
		FirebaseMessaging client = messaging.getObject();
		List<NotificationSendResult> results = fids.stream()
				.gather(Gatherers.windowFixed(BATCH_SIZE))
				.map(batch -> sendBatch(client, batch, hour))
				.toList();
		return summarize(results);
	}

	private NotificationSendResult sendBatch(FirebaseMessaging client, List<String> batch, int hour) {
		List<Message> messages = batch.stream().map(fid -> message(fid, hour)).toList();
		try {
			List<SendResponse> responses = client.sendEach(messages).getResponses();
			List<String> invalidFids = IntStream.range(0, responses.size())
					.filter(index -> isUnregistered(responses.get(index)))
					.mapToObj(batch::get)
					.toList();
			List<FirebaseMessagingException> errors = responses.stream()
					.filter(response -> !response.isSuccessful() && !isUnregistered(response))
					.map(SendResponse::getException)
					.toList();
			int failed = errors.size();
			NotificationDeliveryException failure = errors.stream().findFirst()
					.map(NotificationDeliveryException::new).orElse(null);
			return new NotificationSendResult(invalidFids, failed, failure);
		} catch (FirebaseMessagingException exception) {
			List<String> invalidFids = List.of();
			int failed = batch.size();
			var failure = new NotificationDeliveryException(exception);
			return new NotificationSendResult(invalidFids, failed, failure);
		}
	}

	private boolean isUnregistered(SendResponse response) {
		return !response.isSuccessful()
				&& response.getException().getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED;
	}

	private NotificationSendResult summarize(List<NotificationSendResult> results) {
		List<String> invalidFids = results.stream().flatMap(result -> result.invalidFids().stream()).toList();
		int failed = results.stream().mapToInt(NotificationSendResult::failed).sum();
		NotificationDeliveryException failure = results.stream().map(NotificationSendResult::failure)
				.filter(Objects::nonNull).findFirst().orElse(null);
		return new NotificationSendResult(invalidFids, failed, failure);
	}

	private Message message(String fid, int hour) {
		List<String> text = TEXTS.getOrDefault(hour, List.of("훈련 시간이에요", "오늘의 훈련을 시작해 볼까요?"));
		return Message.builder()
				.setFid(fid)
				.setNotification(Notification.builder().setTitle(text.getFirst()).setBody(text.getLast()).build())
				.putData("hour", String.valueOf(hour))
				.putData("deepLink", "meongcoach://training-curriculum")
				.setAndroidConfig(AndroidConfig.builder().setTtl(3_600_000).setPriority(AndroidConfig.Priority.HIGH).build())
				.setApnsConfig(ApnsConfig.builder().setAps(Aps.builder().build())
						.putHeader("apns-priority", "10")
						.putHeader("apns-expiration", String.valueOf(Instant.now().getEpochSecond() + 3600)).build())
				.build();
	}
}
