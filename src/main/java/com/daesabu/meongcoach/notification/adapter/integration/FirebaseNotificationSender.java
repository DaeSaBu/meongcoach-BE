package com.daesabu.meongcoach.notification.adapter.integration;

import com.daesabu.meongcoach.notification.application.required.NotificationSender;
import com.daesabu.meongcoach.notification.domain.NotificationSendResult;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
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
		List<String> invalidFids = new ArrayList<>();
		int failed = 0;
		FirebaseMessaging client = messaging.getObject();
		for (int offset = 0; offset < fids.size(); offset += BATCH_SIZE) {
			List<String> batch = fids.subList(offset, Math.min(offset + BATCH_SIZE, fids.size()));
			List<Message> messages = batch.stream().map(fid -> message(fid, hour)).toList();
			try {
				var responses = client.sendEach(messages).getResponses();
				for (int i = 0; i < responses.size(); i++) {
					var response = responses.get(i);
					if (response.isSuccessful()) {
						continue;
					}
					if (response.getException().getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
						invalidFids.add(batch.get(i));
						continue;
					}
					log.warn("FCM delivery failed code={}", response.getException().getMessagingErrorCode());
					failed++;
				}
			} catch (FirebaseMessagingException e) {
				log.warn("FCM batch failed code={} size={}", e.getMessagingErrorCode(), batch.size());
				failed += batch.size();
			}
		}
		return new NotificationSendResult(invalidFids, failed);
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
