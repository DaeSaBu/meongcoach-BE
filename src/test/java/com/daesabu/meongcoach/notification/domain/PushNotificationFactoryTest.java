package com.daesabu.meongcoach.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationDeepLinkException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PushNotificationFactoryTest {

	@Test
	void 시각별_문구와_예약_시각_묶음_키로_정기_알림을_만든다() {
		var dispatch = ScheduledNotificationDispatch.of(new ScheduledNotificationDispatchCommand(LocalDate.of(2026, 10, 31), 17, 0), Instant.parse("2026-10-31T08:00:00Z"));

		PushNotification notification = PushNotificationFactory.scheduled(List.of("fid"), dispatch).getFirst();

		assertThat(notification.fid()).isEqualTo("fid");
		assertThat(notification.title()).isEqualTo("저녁 훈련 시간이에요");
		assertThat(notification.body()).isEqualTo("오늘의 훈련을 마무리해 볼까요?");
		assertThat(notification.type()).isNull();
		assertThat(notification.hour()).isEqualTo(17);
		assertThat(notification.deepLink()).isEqualTo(new NotificationDeepLink("meongcoach://training-curriculum"));
		assertThat(notification.collapseKey()).isEqualTo("2026-10-31T08:00:00Z");
	}

	@Test
	void 문구가_없는_시각은_기본_문구를_쓴다() {
		var dispatch = ScheduledNotificationDispatch.of(new ScheduledNotificationDispatchCommand(LocalDate.of(2026, 10, 31), 9, 0), Instant.parse("2026-10-31T00:00:00Z"));

		assertThat(PushNotificationFactory.scheduled(List.of("fid"), dispatch).getFirst().title()).isEqualTo("훈련 시간이에요");
	}

	@Test
	void 앱_스킴이_아닌_딥링크면_던진다() {
		assertThatThrownBy(() -> new NotificationDeepLink("https://evil.example"))
				.isInstanceOf(InvalidNotificationDeepLinkException.class);
	}

	@Test
	void 종류와_딥링크를_데이터에_넣어_이벤트_알림을_만든다() {
		PushNotification notification = PushNotificationFactory.event(
				List.of("fid"), NotificationEventType.OPERATOR_MESSAGE, "제목", "본문", new NotificationDeepLink("meongcoach://notice")).getFirst();

		assertThat(notification.fid()).isEqualTo("fid");
		assertThat(notification.type()).isEqualTo(NotificationEventType.OPERATOR_MESSAGE);
		assertThat(notification.hour()).isNull();
		assertThat(notification.deepLink()).isEqualTo(new NotificationDeepLink("meongcoach://notice"));
		assertThat(notification.collapseKey()).isNotBlank();
	}

	@Test
	void 이벤트_알림은_한_발송의_묶음_키를_공유한다() {
		List<PushNotification> notifications = PushNotificationFactory.event(
				List.of("fid-1", "fid-2"), NotificationEventType.COMMENT, "제목", "본문", new NotificationDeepLink(null));

		assertThat(notifications).extracting(PushNotification::collapseKey).containsOnly(notifications.getFirst().collapseKey());
	}

	@Test
	void 딥링크가_없으면_종류만_넣는다() {
		PushNotification notification = PushNotificationFactory.event(
				List.of("fid"), NotificationEventType.COMMENT, "제목", "본문", new NotificationDeepLink(null)).getFirst();

		assertThat(notification.type()).isEqualTo(NotificationEventType.COMMENT);
		assertThat(notification.deepLink().value()).isNull();
	}
}
