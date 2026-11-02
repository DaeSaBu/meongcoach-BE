package com.daesabu.meongcoach.notification.domain;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationDispatchTimeException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;

@Embeddable
public record NotificationSlot(@Column(name = "scheduled_at", nullable = false, updatable = false) Instant scheduledAt) {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final Duration MAX_DELAY = Duration.ofHours(1);
	private static final String DEEP_LINK = "meongcoach://training-curriculum";
	private static final Map<Integer, Map.Entry<String, String>> MESSAGES = Map.of(
			8, Map.entry("아침 훈련 시간이에요", "오늘의 훈련을 시작해 볼까요?"),
			11, Map.entry("점심 훈련 시간이에요", "잠깐 짬을 내서 한 가지만 연습해 볼까요?"),
			17, Map.entry("저녁 훈련 시간이에요", "오늘의 훈련을 마무리해 볼까요?")
	);
	private static final Map.Entry<String, String> DEFAULT_MESSAGE = Map.entry("훈련 시간이에요", "오늘의 훈련을 시작해 볼까요?");

	public static NotificationSlot of(LocalDate date, NotificationTime time) {
		return new NotificationSlot(date.atTime(time.hour(), time.minute()).atZone(SEOUL).toInstant());
	}

	public NotificationTime time() {
		LocalTime local = scheduledAt.atZone(SEOUL).toLocalTime();
		return new NotificationTime(local.getHour(), local.getMinute());
	}

	public void validateDispatchable(Instant now) {
		if (scheduledAt.isAfter(now) || scheduledAt.isBefore(now.minus(MAX_DELAY))) {
			throw new InvalidNotificationDispatchTimeException(scheduledAt);
		}
	}

	public PushNotification toPushNotification(String fid) {
		Map.Entry<String, String> message = MESSAGES.getOrDefault(time().hour(), DEFAULT_MESSAGE);
		Map<String, String> data = Map.of("hour", String.valueOf(time().hour()), "deepLink", DEEP_LINK);
		return new PushNotification(fid, message.getKey(), message.getValue(), data, scheduledAt.toString());
	}
}
