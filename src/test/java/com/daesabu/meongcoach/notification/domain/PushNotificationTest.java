package com.daesabu.meongcoach.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationDeepLinkException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PushNotificationTest {

	@Test
	void 앱_스킴이_아닌_딥링크면_던진다() {
		assertThatThrownBy(() -> new NotificationDeepLink("https://evil.example"))
				.isInstanceOf(InvalidNotificationDeepLinkException.class);
	}

	@Test
	void 종류와_딥링크를_데이터에_넣는다() {
		PushNotification notification = PushNotification.ofEvent(
				"fid", NotificationEventType.OPERATOR_MESSAGE, "제목", "본문", new NotificationDeepLink("meongcoach://notice"));

		assertThat(notification.fid()).isEqualTo("fid");
		assertThat(notification.data()).isEqualTo(Map.of("type", "OPERATOR_MESSAGE", "deepLink", "meongcoach://notice"));
		assertThat(notification.collapseKey()).isNotBlank();
	}

	@Test
	void 딥링크가_없으면_종류만_넣는다() {
		PushNotification notification = PushNotification.ofEvent(
				"fid", NotificationEventType.COMMENT, "제목", "본문", new NotificationDeepLink(null));

		assertThat(notification.data()).isEqualTo(Map.of("type", "COMMENT"));
	}
}
