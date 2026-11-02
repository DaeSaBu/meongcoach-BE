package com.daesabu.meongcoach.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PushNotificationTest {

	@Test
	void 정기_알림은_시각과_딥링크를_데이터로_만든다() {
		var notification = new PushNotification("fid", "제목", "본문", null, 17,
				new NotificationDeepLink("meongcoach://training-curriculum"), "key");

		assertThat(notification.data()).isEqualTo(Map.of("hour", "17", "deepLink", "meongcoach://training-curriculum"));
	}

	@Test
	void 이벤트_알림은_종류와_딥링크를_데이터로_만들고_없는_값은_넣지_않는다() {
		var withDeepLink = new PushNotification("fid", "제목", "본문", NotificationEventType.OPERATOR_MESSAGE, null,
				new NotificationDeepLink("meongcoach://notice"), "key");
		var withoutDeepLink = new PushNotification("fid", "제목", "본문", NotificationEventType.COMMENT, null,
				new NotificationDeepLink(null), "key");

		assertThat(withDeepLink.data()).isEqualTo(Map.of("type", "OPERATOR_MESSAGE", "deepLink", "meongcoach://notice"));
		assertThat(withoutDeepLink.data()).isEqualTo(Map.of("type", "COMMENT"));
	}
}
