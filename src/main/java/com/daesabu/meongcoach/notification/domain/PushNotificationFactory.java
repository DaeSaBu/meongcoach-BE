package com.daesabu.meongcoach.notification.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PushNotificationFactory {

	private static final String TRAINING_DEEP_LINK = "meongcoach://training-curriculum";
	private static final Map<Integer, Map.Entry<String, String>> TRAINING_MESSAGES = Map.of(
			8, Map.entry("아침 훈련 시간이에요", "오늘의 훈련을 시작해 볼까요?"),
			11, Map.entry("점심 훈련 시간이에요", "잠깐 짬을 내서 한 가지만 연습해 볼까요?"),
			17, Map.entry("저녁 훈련 시간이에요", "오늘의 훈련을 마무리해 볼까요?")
	);
	private static final Map.Entry<String, String> DEFAULT_TRAINING_MESSAGE = Map.entry("훈련 시간이에요", "오늘의 훈련을 시작해 볼까요?");

	public static List<PushNotification> scheduled(List<String> fids, NotificationSlot slot) {
		int hour = slot.time().hour();
		Map.Entry<String, String> message = TRAINING_MESSAGES.getOrDefault(hour, DEFAULT_TRAINING_MESSAGE);
		Map<String, String> data = Map.of("hour", String.valueOf(hour), "deepLink", TRAINING_DEEP_LINK);
		String collapseKey = slot.scheduledAt().toString();
		return fids.stream()
				.map(fid -> new PushNotification(fid, message.getKey(), message.getValue(), data, collapseKey))
				.toList();
	}

	public static List<PushNotification> event(List<String> fids, NotificationEventType type, String title, String body, NotificationDeepLink deepLink) {
		Map<String, String> mutableData = new HashMap<>();
		mutableData.put("type", type.name());
		if (deepLink.value() != null) {
			mutableData.put("deepLink", deepLink.value());
		}
		Map<String, String> data = Map.copyOf(mutableData);
		return fids.stream()
				.map(fid -> new PushNotification(fid, title, body, data, UUID.randomUUID().toString()))
				.toList();
	}
}
