package com.daesabu.meongcoach.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationDeepLinkException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PushNotificationFactoryTest {

	@Test
	void 시각별_문구와_예약_시각_묶음_키로_정기_알림을_만든다() {
		NotificationSlot slot = NotificationSlot.of(LocalDate.of(2026, 10, 31), new NotificationTime(17, 0));

		PushNotification notification = PushNotificationFactory.scheduled(List.of("fid"), slot).getFirst();

		assertThat(notification.fid()).isEqualTo("fid");
		assertThat(notification.title()).isEqualTo("저녁 훈련 시간이에요");
		assertThat(notification.body()).isEqualTo("오늘의 훈련을 마무리해 볼까요?");
		assertThat(notification.data()).isEqualTo(Map.of("hour", "17", "deepLink", "meongcoach://training-curriculum"));
		assertThat(notification.collapseKey()).isEqualTo("2026-10-31T08:00:00Z");
	}

	@Test
	void 문구가_없는_시각은_기본_문구를_쓴다() {
		NotificationSlot slot = NotificationSlot.of(LocalDate.of(2026, 10, 31), new NotificationTime(9, 0));

		assertThat(PushNotificationFactory.scheduled(List.of("fid"), slot).getFirst().title()).isEqualTo("훈련 시간이에요");
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
		assertThat(notification.data()).isEqualTo(Map.of("type", "OPERATOR_MESSAGE", "deepLink", "meongcoach://notice"));
		assertThat(notification.collapseKey()).isNotBlank();
	}

	@Test
	void 딥링크가_없으면_종류만_넣는다() {
		PushNotification notification = PushNotificationFactory.event(
				List.of("fid"), NotificationEventType.COMMENT, "제목", "본문", new NotificationDeepLink(null)).getFirst();

		assertThat(notification.data()).isEqualTo(Map.of("type", "COMMENT"));
	}
}
